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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.websocket.WebSocket;
import org.xwiki.contrib.websocket.WebSocketHandler;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;

/**
 * {@link WebSocketHandler} implementation that simply echoes the messages it receives.
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named("echo")
public class EchoWebSocketHandler implements WebSocketHandler
{
    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private ModelContext modelContext;

    @Override
    public void onConnect(WebSocket webSocket)
    {
        String currentWiki = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
        webSocket.onMessage(message -> webSocket
            .send(String.format("[%s] %s -> %s", currentWiki, this.bridge.getCurrentUserReference(), message)));
    }
}
