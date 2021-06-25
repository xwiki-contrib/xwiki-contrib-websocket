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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.websocket.WebSocketConfig;
import org.xwiki.contrib.websocket.WebSocketHandler;
import org.xwiki.contrib.websocket.internal.WebSocketURI;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.script.service.ScriptService;

/**
 * Exposes WebSocket related APIs to server-side scripts.
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named("websocket")
public class WebSocketScriptService implements ScriptService
{
    @Inject
    private WebSocketConfig config;

    @Inject
    private ModelContext modelContext;

    /**
     * Get the URL for accessing the WebSocket. The exact form of this URL results from the WebSocket configuration, the
     * current wiki and the handler component hint.
     * 
     * @param handler the hint of the {@link WebSocketHandler} component implementation
     * @return the URL that can be used to communicate with the specified handler
     */
    public String getURL(String handler)
    {
        String externalPath = this.config.getExternalPath();
        String wiki = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
        return new WebSocketURI(externalPath, wiki, handler).toString();
    }
}
