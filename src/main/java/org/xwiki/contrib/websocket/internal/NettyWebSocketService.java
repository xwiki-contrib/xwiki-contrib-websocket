/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.websocket.internal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.contrib.websocket.WebSocketHandler;
import org.xwiki.model.reference.DocumentReference;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Netty-based {@link WebSocketService} implementation.
 * 
 * @version $Id$
 */
@Component
@Named("netty")
@Singleton
public class NettyWebSocketService implements WebSocketService, Initializable
{
    private Map<String, DocumentReference> userByKeyWiki = new HashMap<String, DocumentReference>();

    private Map<String, String> keyByUserWiki = new HashMap<String, String>();

    @Inject
    private ComponentManagerManager compMgrMgr;

    @Inject
    private WebSocketConfig conf;

    @Inject
    private Logger logger;

    private static String getUserWiki(DocumentReference userRef, String wiki)
    {
        return wiki.length() + ':' + wiki + userRef.toString();
    }

    private static String getKeyWiki(String key, String wiki)
    {
        return wiki.length() + ':' + wiki + key;
    }

    @Override
    public String getKey(String wiki, DocumentReference userRef)
    {
        String userWiki = getUserWiki(userRef, wiki);
        String key = keyByUserWiki.get(userWiki);
        if (key != null) {
            return key;
        }
        key = RandomStringUtils.randomAlphanumeric(20);
        keyByUserWiki.put(userWiki, key);
        userByKeyWiki.put(getKeyWiki(key, wiki), userRef);
        return key;
    }

    @Override
    public DocumentReference getUser(String wiki, String key)
    {
        if (wiki == null || key == null) {
            return null;
        }
        return userByKeyWiki.get(getKeyWiki(key, wiki));
    }

    private void checkCertChainAndPrivKey(File certChain, File privKey)
    {
        if (!certChain.exists()) {
            throw new RuntimeException(
                "SSL enabled with websocket.ssl.certChainFile set but the certChainFile does not seem to exist.");
        }
        if (!privKey.exists()) {
            throw new RuntimeException("SSL enabled with websocket.ssl.certChainFile set "
                + "but the pkcs8PrivateKeyFile does not seem to exist.");
        }
        String privKeyStr;
        try {
            privKeyStr = FileUtils.readFileToString(privKey, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (privKeyStr.indexOf("-----BEGIN PRIVATE KEY-----") == -1) {
            throw new RuntimeException("websocket.ssl.pkcs8PrivateKeyFile does not seem to "
                + "be a PKCS8 private key. The SSLeay format is not " + "supported.");
        }
    }

    private void initializeInternal() throws Exception
    {
        final SslContext sslCtx;
        if (this.conf.sslEnabled()) {
            if (this.conf.getCertChainFilename() != null) {
                // They provided a cert chain filename (and ostensibly a private key)
                // ssl w/ CA signed certificate.
                final File certChain = new File(this.conf.getCertChainFilename());
                final File privKey = new File(this.conf.getPrivateKeyFilename());
                checkCertChainAndPrivKey(certChain, privKey);
                sslCtx = SslContextBuilder.forServer(certChain, privKey).build();
            } else {
                // SSL enabled but no certificate specified, lets use a selfie
                this.logger.warn("websocket.ssl.enable = true but websocket.ssl.certChainFile "
                    + "is unspecified, generating a Self Signed Certificate.");
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            }
        } else {
            sslCtx = null;
        }

        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();

        // get rid of silly lag
        b.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new WebSocketServerInitializer(sslCtx, this));

        Channel ch = b.bind(this.conf.getBindTo(), this.conf.getPort()).sync().channel();

        ch.closeFuture().addListener(new GenericFutureListener<ChannelFuture>()
        {
            public void operationComplete(ChannelFuture f)
            {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
    }

    @Override
    public void initialize()
    {
        try {
            initializeInternal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class WebSocketServerInitializer extends ChannelInitializer<SocketChannel>
    {
        private final SslContext sslCtx;

        private final NettyWebSocketService nwss;

        WebSocketServerInitializer(SslContext sslCtx, NettyWebSocketService nwss)
        {
            this.sslCtx = sslCtx;
            this.nwss = nwss;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception
        {
            ChannelPipeline pipeline = ch.pipeline();
            if (sslCtx != null) {
                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
            }
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(65536));
            pipeline.addLast(new WebSocketServerHandler(this.nwss));
        }
    }

    private static final class WebSocketServerHandler extends SimpleChannelInboundHandler<Object>
    {
        private final NettyWebSocketService nwss;

        private WebSocketServerHandshaker handshaker;

        private NettyWebSocket nws;

        private StringBuilder frames;

        WebSocketServerHandler(NettyWebSocketService nwss)
        {
            this.nwss = nwss;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg)
        {
            if (msg instanceof FullHttpRequest) {
                handleHttpRequest(ctx, (FullHttpRequest) msg);
            } else if (msg instanceof WebSocketFrame) {
                handleWebSocketFrame(ctx, (WebSocketFrame) msg);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx)
        {
            ctx.flush();
        }

        private void handleHttpReqB(ChannelHandlerContext ctx, FullHttpRequest req, WebSocketRequest wsRequest)
        {
            ComponentManager cm = this.nwss.compMgrMgr.getComponentManager("wiki:" + wsRequest.getWiki(), false);
            if (cm == null) {
                ByteBuf content = Unpooled.copiedBuffer("ERROR: no wiki found named [" + wsRequest.getWiki() + "]",
                    StandardCharsets.UTF_8);
                sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, content));
                return;
            }

            WebSocketHandler handler = null;
            try {
                handler = cm.getInstance(WebSocketHandler.class, wsRequest.getHandlerName());
            } catch (Exception e) {
                // fall through
            }
            if (handler == null) {
                ByteBuf content = Unpooled.copiedBuffer(
                    "ERROR: no registered component for path [" + wsRequest.getHandlerName() + "]",
                    StandardCharsets.UTF_8);
                sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, content));
                return;
            }

            String loc = getWebSocketLocation(req, this.nwss.conf.sslEnabled());
            WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(loc, null, false, this.nwss.conf.maxFrameSize());
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
            }

            this.nws = new NettyWebSocket(wsRequest, ctx);

            try {
                handler.onWebSocketConnect(this.nws);
            } catch (Exception e) {
                this.nwss.logger.warn("Exception in {}.onWebSocketConnect()... [{}]", handler.getClass().getName(),
                    ExceptionUtils.getStackTrace(e));
            }

            ctx.channel().closeFuture().addListener(new GenericFutureListener<ChannelFuture>()
            {
                public void operationComplete(ChannelFuture f)
                {
                    nws.disconnect();
                }
            });
        }

        private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req)
        {
            // Handle a bad request.
            if (!req.decoderResult().isSuccess()) {
                sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
                return;
            }

            // Form: /wiki/handler?k=12345
            WebSocketRequest wsRequest = new NettyWebSocketRequest(req, this.nwss);

            if (req.method() != HttpMethod.GET) {
                this.nwss.logger.debug("request method not GET");
            } else if (!wsRequest.isValid()) {
                this.nwss.logger.debug("invalid URI, parsing failed");
            } else {
                // success
                handleHttpReqB(ctx, req, wsRequest);
                return;
            }
            // failure
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
        }

        private synchronized void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame)
        {
            // Check for closing frame
            if (frame instanceof CloseWebSocketFrame) {
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
                return;
            }
            if (frame instanceof PingWebSocketFrame) {
                ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
                return;
            }
            if (!frame.isFinalFragment() || this.frames != null) {
                final String msg;
                if (frame instanceof TextWebSocketFrame) {
                    msg = ((TextWebSocketFrame) frame).text();
                } else if (frame instanceof ContinuationWebSocketFrame) {
                    msg = ((ContinuationWebSocketFrame) frame).text();
                } else {
                    throw new UnsupportedOperationException(
                        "unsupported frame fragment type " + frame.getClass().getName());
                }
                if (this.frames == null) {
                    this.frames = new StringBuilder();
                }
                this.frames.append(msg);
                if (this.frames.length() > this.nwss.conf.maxFrameSize()) {
                    throw new RuntimeException("Frame size too big [" + this.frames.length() + "] max frame size ["
                        + this.nwss.conf.maxFrameSize() + "]");
                }
                if (frame.isFinalFragment()) {
                    final String fullMsg = this.frames.toString();
                    this.frames = null;
                    this.nws.message(fullMsg);
                }
                return;
            }
            if (!(frame instanceof TextWebSocketFrame)) {
                throw new UnsupportedOperationException(
                    String.format("%s frame types not supported", frame.getClass().getName()));
            }

            final String msg = ((TextWebSocketFrame) frame).text();
            this.nws.message(msg);
        }

        private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res)
        {
            // Generate an error page if response getStatus code is not OK (200).
            if (res.status().code() != 200) {
                ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
                res.content().writeBytes(buf);
                buf.release();
                HttpUtil.setContentLength(res, res.content().readableBytes());
            }

            // Send the response and close the connection if necessary.
            ChannelFuture f = ctx.channel().writeAndFlush(res);
            if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        {
            this.nwss.logger.warn("Netty exceptionCaught() [{}]", ExceptionUtils.getStackTrace(cause));
            ctx.close();
        }

        private static String getWebSocketLocation(FullHttpRequest req, boolean ssl)
        {
            String location = req.headers().get(HttpHeaderNames.HOST) + "/";
            if (ssl) {
                return "wss://" + location;
            } else {
                return "ws://" + location;
            }
        }
    }
}
