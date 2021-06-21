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

import java.util.List;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * The WebSocket which is passed to the registered {@link WebSocketHandler}.
 * 
 * @version $Id$
 */
public interface WebSocket
{
    /**
     * @return the user who accessed the WebSocket
     */
    DocumentReference getUser();

    /**
     * @return identifies the WebSocket handler using the component role hint
     */
    String getPath();

    /**
     * @return the wiki where this WebSocket was registered
     */
    String getWiki();

    /**
     * @return the query string parameters received with the connection request
     */
    Map<String, List<String>> getParameters();

    /**
     * Sends a message on the WebSocket.
     * 
     * @param message the message to send
     */
    void send(String message);

    /**
     * Call this inside the {@link #onMessage(Callback)} callback to access the received message.
     * 
     * @return the received message, when called from {@link #onMessage(Callback)}, {@code null} otherwise
     */
    String recv();

    /**
     * Execute some code when WebSocket messages are received.
     * 
     * @param callback the callback to be called when a WebSocket message comes in for this handler
     */
    void onMessage(Callback callback);

    /**
     * Execute some code when the client disconnects.
     * 
     * @param callback the callback to be called when it is detected that the client has disconnected
     */
    void onDisconnect(Callback callback);

    /**
     * A callback function as an object.
     */
    interface Callback
    {
        /**
         * @param webSocket the WebSocket that triggered the event (callback)
         */
        void call(WebSocket webSocket);
    }
}
