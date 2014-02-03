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
    private Container cont;

    public String getURL(String path)
    {
        String host = sock.getExternalHost();
        if (host == null) {
            HttpServletRequest hsr =
                ((ServletRequest) this.cont.getRequest()).getHttpServletRequest();
            host = hsr.getHeader("host");
            if (host.indexOf(':') != -1) {
                host = host.substring(0, host.indexOf(':'));
            }
        }
        DocumentReference user = this.bridge.getCurrentUserReference();
        if (user == null) { user = GUEST_USER; }
        return "ws://" + host + ":" + sock.getPort() + "/" + path + "?k=" + this.sock.getKey(user);
    }
}
