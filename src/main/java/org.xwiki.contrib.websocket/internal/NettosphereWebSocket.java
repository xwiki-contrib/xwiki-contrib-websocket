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

import java.util.List;
import java.util.ArrayList;
import javax.inject.Inject;

import org.atmosphere.cpr.AtmosphereResource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.contrib.websocket.WebSocket;

public class NettosphereWebSocket implements WebSocket
{
    private final String key;
    private final DocumentReference user;
    private final String path;
    private final AtmosphereResource ar;
    private String currentMessage;
    private final List<WebSocket.Callback> messageHandlers = new ArrayList<WebSocket.Callback>();
    private final List<WebSocket.Callback> disconnectHandlers = new ArrayList<WebSocket.Callback>();

    NettosphereWebSocket(DocumentReference user, String path, AtmosphereResource ar, String key) {
        this.user = user;
        this.path = path;
        this.ar = ar;
        this.key = key;
    }

    public DocumentReference getUser() { return this.user; }
    public String getPath() { return this.path; }

    public void send(String message)
    {
        ar.getResponse().write(message);
    }

    public String recv() { return this.currentMessage; }

    public void onMessage(WebSocket.Callback cb) { this.messageHandlers.add(cb); }
    public void onDisconnect(WebSocket.Callback cb) { this.disconnectHandlers.add(cb); }

    void message(String msg)
    {
        for (WebSocket.Callback cb : this.messageHandlers) {
            this.currentMessage = msg;
            cb.call(this);
            this.currentMessage = null;
        }
    }

    void disconnect()
    {
        for (WebSocket.Callback cb : this.disconnectHandlers) {
            cb.call(this);
        }
    }

    String uuid() { return this.ar.uuid(); }
    String getKey() { return this.key; }
}
