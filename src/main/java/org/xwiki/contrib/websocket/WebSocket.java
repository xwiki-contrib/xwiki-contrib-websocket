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
 * The WebSocket which is passed to the registered WebSocketHandler.
 * 
 * @version $Id$
 */
public interface WebSocket
{
    /** @return The user who accessed the websocket. */
    DocumentReference getUser();

    /** @return the *handler* (path is a misleading statement). */
    String getPath();

    /** @return the wiki where this websocket was registered. */
    String getWiki();

    /** @return the query string parameters received from connection request. */
    Map<String, List<String>> getParameters();

    /** @param message send a message on the websocket */
    void send(String message);

    /** @return null unless inside of onMessage() callback in which case return */
    String recv();

    /** @param cb a Callback to be called when a websocket message comes in for this handler. */
    void onMessage(Callback cb);

    /** @param cb a Callback to be called when it is detected that the client has disconnected. */
    void onDisconnect(Callback cb);

    /**
     * A Callback Function as an object.
     */
    interface Callback
    {
        /** ... */
        void call(WebSocket ws);
    }
}
