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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.contrib.websocket.WebSocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * {@link WebSocket} implementation based on Netty.
 * 
 * @version $Id$
 */
public class NettyWebSocket implements WebSocket
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyWebSocket.class);

    private final ChannelHandlerContext context;

    private final List<Consumer<String>> messageHandlers = new ArrayList<>();

    private final List<Runnable> disconnectHandlers = new ArrayList<>();

    NettyWebSocket(ChannelHandlerContext context)
    {
        this.context = context;
    }

    @Override
    public void send(String message)
    {
        this.context.channel().writeAndFlush(new TextWebSocketFrame(message));
    }

    @Override
    public void onMessage(Consumer<String> messageHandler)
    {
        this.messageHandlers.add(messageHandler);
    }

    @Override
    public void onDisconnect(Runnable callback)
    {
        this.disconnectHandlers.add(callback);
    }

    void message(String message)
    {
        for (Consumer<String> messageHandler : this.messageHandlers) {
            try {
                messageHandler.accept(message);
            } catch (Exception e) {
                LOGGER.warn("Exception in WebSocket.onMessage(). Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    void disconnect()
    {
        for (Runnable callback : this.disconnectHandlers) {
            try {
                callback.run();
            } catch (Exception e) {
                LOGGER.warn("Exception in WebSocket.onDisconnect(). Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
