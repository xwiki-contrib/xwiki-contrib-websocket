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
package org.xwiki.contrib.websocket;

import java.util.function.Consumer;

/**
 * The interface used by a {@link WebSocketHandler} to communicate with the client (receive and send back messages).
 * 
 * @version $Id$
 */
public interface WebSocket
{
    /**
     * Sends a message on this WebSocket.
     * 
     * @param message the message to send
     */
    void send(String message);

    /**
     * Execute some code each time a message is received on this WebSocket.
     * 
     * @param messageHandler the code that handles the received message
     */
    void onMessage(Consumer<String> messageHandler);

    /**
     * Execute some code when the client disconnects from this WebSocket.
     * 
     * @param callback the code to execute when the client disconnects
     */
    void onDisconnect(Runnable callback);
}
