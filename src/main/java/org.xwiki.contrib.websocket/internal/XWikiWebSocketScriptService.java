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
import javax.servlet.http.HttpServletRequest;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.contrib.websocket.WebSocketService;
import org.xwiki.contrib.websocket.WebSocketHandler;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.internal.multi.ComponentManagerManager;

@Component
@Named("websocket")
public class XWikiWebSocketScriptService implements ScriptService
{
    public static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", "XWikiGuest");

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    @Named("nettosphere")
    private WebSocketService sock;

    @Inject
    private ComponentManagerManager compMgrMgr;

    @Inject
    private Container cont;

    /**
     * This will throw an error if the component does not exist which is more helpful than
     * the error being thrown somewhere deep inside of the websocket infra where it can only
     * be printed to the log.
     */
    private void checkHandlerExists(String wiki, String handler)
    {
        ComponentManager cm = this.compMgrMgr.getComponentManager("wiki:" + wiki, false);
        if (cm == null) {
            throw new RuntimeException("Could not find ComponentManager for this wiki.");
        }
        try {
            cm.getInstance(WebSocketHandler.class, handler);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Could not find a WebSocketHandler for [" + handler + "]");
        }
    }

    public String getURL(String handlerName)
    {
        String wiki = this.bridge.getCurrentDocumentReference().getRoot().getName();

        checkHandlerExists(wiki, handlerName);

        String externalPath = sock.getExternalPath();
        if (externalPath == null) {
            HttpServletRequest hsr =
                ((ServletRequest) this.cont.getRequest()).getHttpServletRequest();

            String host = hsr.getHeader("host");
            if (host.indexOf(':') != -1) {
                host = host.substring(0, host.indexOf(':'));
            }

            externalPath = "ws://" + host + ":" + sock.getPort() + "/";
        }
        if (!externalPath.endsWith("/")) { externalPath += "/"; }
        DocumentReference user = this.bridge.getCurrentUserReference();
        if (user == null) { user = GUEST_USER; }
        return externalPath + wiki + "/" + handlerName + "?k=" + this.sock.getKey(user);
    }
}
