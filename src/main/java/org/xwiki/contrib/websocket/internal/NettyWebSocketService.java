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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.contrib.websocket.WebSocketConfig;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Netty-based WebSocket service implementation.
 * 
 * @version $Id$
 */
@Component(roles = NettyWebSocketService.class)
@Singleton
public class NettyWebSocketService implements Initializable, Disposable
{
    private final class WebSocketServerInitializer extends ChannelInitializer<SocketChannel>
    {
        private final SslContext sslContext;

        WebSocketServerInitializer(SslContext sslContext)
        {
            this.sslContext = sslContext;
        }

        @Override
        public void initChannel(SocketChannel channel) throws Exception
        {
            ChannelPipeline pipeline = channel.pipeline();
            if (sslContext != null) {
                pipeline.addLast(sslContext.newHandler(channel.alloc()));
            }
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(65536));
            pipeline.addLast(requestHandlerProvider.get());
        }
    }

    @Inject
    private WebSocketConfig config;

    @Inject
    private Logger logger;

    @Inject
    private Provider<NettyWebSocketRequestHandler> requestHandlerProvider;

    private Channel channel;

    @Override
    public void initialize()
    {
        try {
            initializeInternal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeInternal() throws Exception
    {
        SslContext sslContext = null;
        if (this.config.sslEnabled()) {
            if (this.config.getCertChainFilename() != null) {
                // Check the provided a certificate chain file and private key.
                final File certificateChain = new File(this.config.getCertChainFilename());
                final File privateKey = new File(this.config.getPrivateKeyFilename());
                checkCertChainAndPrivKey(certificateChain, privateKey);
                // Create the SSL context based on the provided CA signed certificate.
                sslContext = SslContextBuilder.forServer(certificateChain, privateKey).build();
            } else {
                // SSL enabled but no certificate specified, lets create one.
                this.logger.warn("websocket.ssl.enable = true but websocket.ssl.certChainFile "
                    + "is unspecified, generating a Self Signed Certificate.");
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            }
        }

        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();

        // Get rid of silly lag.
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);

        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new WebSocketServerInitializer(sslContext));

        this.channel = serverBootstrap.bind(this.config.getBindTo(), this.config.getPort()).sync().channel();

        this.channel.closeFuture().addListener(channelFuture -> {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        });
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

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.channel.close();
    }
}
