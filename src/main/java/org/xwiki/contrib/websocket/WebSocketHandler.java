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

import org.xwiki.component.annotation.Role;

/**
 * Implement this role in order to provide a WebSocket service. Here's an example of how you can implement a simple
 * WebSocket handler that echoes the message it receives:
 * 
 * <pre>
 * &#64;Component
 * &#64;Singleton
 * &#64;Named("echo")
 * public class EchoWebSocketHandler implements WebSocketHandler
 * {
 *     &#64;Override
 *     public void onConnect(WebSocket webSocket)
 *     {
 *         webSocket.onMessage(message -&#62; webSocket.send(message));
 *     }
 * }
 * </pre>
 * 
 * @version $Id$
 */
@Role
public interface WebSocketHandler
{
    /**
     * Called when a connection to this WebSocket service has been made. Use the passed WebSocket to communicate with
     * the connected peer (send and receive messages).
     * 
     * @param webSocket the connected WebSocket to use for communication
     */
    void onConnect(WebSocket webSocket);
}
