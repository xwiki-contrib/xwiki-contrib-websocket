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

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.websocket.WebSocketConfig;
import org.xwiki.contrib.websocket.WebSocketHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
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

/**
 * Dispatches the WebSocket requests to the right {@link WebSocketHandler}.
 * 
 * @version $Id$
 */
@Component(roles = NettyWebSocketRequestHandler.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public final class NettyWebSocketRequestHandler extends SimpleChannelInboundHandler<Object>
{
    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private WebSocketContextInitializer xcontextInitializer;

    @Inject
    private WebSocketConfig config;

    private WebSocketServerHandshaker handShaker;

    private NettyWebSocket webSocket;

    private StringBuilder frames;

    @Override
    public void channelRead0(ChannelHandlerContext context, Object message)
    {
        if (message instanceof FullHttpRequest) {
            handleHttpRequest(context, (FullHttpRequest) message);
        } else if (message instanceof WebSocketFrame) {
            handleWebSocketFrame(context, (WebSocketFrame) message);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context)
    {
        context.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext context, FullHttpRequest request)
    {
        // Handle a bad request.
        if (!request.decoderResult().isSuccess()) {
            sendHttpResponse(context, request,
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        } else if (request.method() != HttpMethod.GET) {
            sendHttpResponse(context, request,
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED));
            return;
        }

        NettyXWikiWebSocketRequest xwikiRequest = null;
        try {
            xwikiRequest = new NettyXWikiWebSocketRequest(request, context.channel());
        } catch (URISyntaxException e) {
            this.logger.debug("Invalid WebSocket URI. Root cause is [{}].", ExceptionUtils.getRootCauseMessage(e));
            sendHttpResponse(context, request,
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
        }

        this.xcontextInitializer.initialize(xwikiRequest);

        WebSocketHandler handler = null;
        String handlerName = xwikiRequest.getWebSocketURI().getHandler();
        try {
            handler = this.componentManagerProvider.get().getInstance(WebSocketHandler.class, handlerName);
        } catch (Exception e) {
            ByteBuf content = Unpooled.copiedBuffer("ERROR: no registered component for path [" + handlerName + "]",
                StandardCharsets.UTF_8);
            sendHttpResponse(context, request,
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, content));
            return;
        }

        handleWebSocketHandshake(context, request, handler);
    }

    private void handleWebSocketHandshake(ChannelHandlerContext context, FullHttpRequest request,
        WebSocketHandler handler)
    {
        String location = getWebSocketLocation(request, this.config.sslEnabled());
        WebSocketServerHandshakerFactory wsFactory =
            new WebSocketServerHandshakerFactory(location, null, false, this.config.maxFrameSize());
        this.handShaker = wsFactory.newHandshaker(request);
        if (this.handShaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
        } else {
            this.handShaker.handshake(context.channel(), request);
        }

        this.webSocket = new NettyWebSocket(context);

        try {
            handler.onConnect(this.webSocket);
        } catch (Exception e) {
            this.logger.warn("Exception in {}.onConnect(): [{}]", handler.getClass().getName(),
                ExceptionUtils.getStackTrace(e));
        }

        context.channel().closeFuture().addListener(channelFuture -> this.webSocket.disconnect());
    }

    private synchronized void handleWebSocketFrame(ChannelHandlerContext context, WebSocketFrame frame)
    {
        // Check for closing frame.
        if (frame instanceof CloseWebSocketFrame) {
            handShaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
        } else if (frame instanceof PingWebSocketFrame) {
            context.channel().write(new PongWebSocketFrame(frame.content().retain()));
        } else if (!frame.isFinalFragment() || this.frames != null) {
            handleWebSocketMultipleFrames(context, frame);
        } else if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(
                String.format("%s frame types not supported", frame.getClass().getName()));
        } else {
            // Single text frame.
            final String msg = ((TextWebSocketFrame) frame).text();
            this.webSocket.message(msg);
        }
    }

    private void handleWebSocketMultipleFrames(ChannelHandlerContext context, WebSocketFrame frame)
    {
        final String msg;
        if (frame instanceof TextWebSocketFrame) {
            msg = ((TextWebSocketFrame) frame).text();
        } else if (frame instanceof ContinuationWebSocketFrame) {
            msg = ((ContinuationWebSocketFrame) frame).text();
        } else {
            throw new UnsupportedOperationException("unsupported frame fragment type " + frame.getClass().getName());
        }
        if (this.frames == null) {
            this.frames = new StringBuilder();
        }
        this.frames.append(msg);
        if (this.frames.length() > this.config.maxFrameSize()) {
            throw new RuntimeException("Frame size too big [" + this.frames.length() + "] max frame size ["
                + this.config.maxFrameSize() + "]");
        }
        if (frame.isFinalFragment()) {
            final String fullMsg = this.frames.toString();
            this.frames = null;
            this.webSocket.message(fullMsg);
        }
    }

    private void sendHttpResponse(ChannelHandlerContext context, FullHttpRequest request, FullHttpResponse response)
    {
        // Generate an error page if response getStatus code is not OK (200).
        if (response.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), StandardCharsets.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(response, response.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = context.channel().writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(request) || response.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause)
    {
        this.logger.warn("Netty exceptionCaught() [{}]", ExceptionUtils.getStackTrace(cause));
        context.close();
    }

    private String getWebSocketLocation(FullHttpRequest request, boolean ssl)
    {
        return (ssl ? "wss://" : "ws://") + request.headers().get(HttpHeaderNames.HOST) + "/";
    }
}
