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
package org.xwiki.contrib.websocket.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.websocket.WebSocketHandler;
import org.xwiki.contrib.websocket.internal.WebSocketConfig;
import org.xwiki.contrib.websocket.internal.WebSocketService;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

/**
 * Exposes WebSocket related APIs to server-side scripts.
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named("websocket")
public class XWikiWebSocketScriptService implements ScriptService
{
    private static final DocumentReference GUEST_USER = new DocumentReference("xwiki", "XWiki", "XWikiGuest");

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    @Named("netty")
    private WebSocketService webSocketService;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private ModelContext modelContext;

    @Inject
    private WebSocketConfig config;

    /**
     * Get the URL for accessing the WebSocket. The exact form of this URL results from the WebSocket configuration, the
     * current wiki and the handlerName as well as the key which grants the user authorization to access the WebSocket.
     * 
     * @param handlerName the handler name (component hint)
     * @return the URL that can be used to communicate with the specified handler
     */
    public String getURL(String handlerName)
    {
        checkHandlerExists(handlerName);

        String externalPath = this.config.getExternalPath();
        String wiki = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
        return externalPath + wiki + '/' + handlerName + "?k=" + this.webSocketService.getKey(wiki, getUser());
    }

    /**
     * This will throw an error if the component does not exist which is more helpful than the error being thrown
     * somewhere deep inside of the WebSocket infrastructure where it can only be printed to the log.
     */
    private void checkHandlerExists(String handler)
    {
        try {
            this.componentManagerProvider.get().getInstance(WebSocketHandler.class, handler);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Could not find a WebSocketHandler for [" + handler + "].");
        }
    }

    private DocumentReference getUser()
    {
        DocumentReference user = this.bridge.getCurrentUserReference();
        if (user == null) {
            user = GUEST_USER;
        }
        return user;
    }
}
