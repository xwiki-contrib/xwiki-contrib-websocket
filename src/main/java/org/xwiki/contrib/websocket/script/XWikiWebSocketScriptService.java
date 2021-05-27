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

import java.security.MessageDigest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.RandomStringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.contrib.websocket.WebSocketHandler;
import org.xwiki.contrib.websocket.internal.WebSocketConfig;
import org.xwiki.contrib.websocket.internal.WebSocketService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

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
    private WebSocketService sock;

    @Inject
    private ComponentManagerManager compMgrMgr;

    @Inject
    private Container cont;

    @Inject
    private WebSocketConfig conf;

    @Inject
    private AuthorizationManager authMgr;

    /** The master key used for creation of document keys. */
    private String secret = RandomStringUtils.randomAlphanumeric(32);

    /**
     * This will throw an error if the component does not exist which is more helpful than the error being thrown
     * somewhere deep inside of the WebSocket infra where it can only be printed to the log.
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

    private DocumentReference getUser()
    {
        DocumentReference user = this.bridge.getCurrentUserReference();
        if (user == null) {
            user = GUEST_USER;
        }
        return user;
    }

    /**
     * Get the URL for accessing the WebSocket. The exact form of this URL results from the WebSocket configuration, the
     * current wiki and the handlerName as well as the key which grants the user authorization to access the WebSocket.
     * 
     * @param handlerName the handler name (component hint)
     * @return the URL that can be used to communicate with the specified handler
     */
    public String getURL(String handlerName)
    {
        String wiki = this.bridge.getCurrentDocumentReference().getRoot().getName();

        checkHandlerExists(wiki, handlerName);

        String externalPath = this.conf.getExternalPath();
        if (externalPath == null) {
            HttpServletRequest hsr = ((ServletRequest) this.cont.getRequest()).getHttpServletRequest();

            String host = hsr.getHeader("host");
            if (host.indexOf(':') != -1) {
                host = host.substring(0, host.indexOf(':'));
            }

            String proto = "ws";
            if (this.conf.sslEnabled()) {
                proto = "wss";
            }

            externalPath = String.format("%s://%s:%s/", proto, host, this.conf.getPort());
        } else if (!externalPath.endsWith("/")) {
            externalPath += '/';
        }
        return externalPath + wiki + '/' + handlerName + "?k=" + this.sock.getKey(wiki, getUser());
    }

    /**
     * Get a token which corresponds to a document reference. If the current user does not have permission to access the
     * document, this function will return the string "ENOPERM". The motivation is to allow a secret value for
     * encryption or real-time channel creation so that users who are authorized and able to access the WebSocket are
     * still not able to join WebSocket sessions based on documents for which they do not have edit access.
     *
     * @param ref a reference to the document to check.
     * @return a base64 string or, if the user does not have access, "ENOPERM".
     */
    public String getDocumentKey(DocumentReference ref)
    {
        if (!this.authMgr.hasAccess(Right.EDIT, getUser(), ref)) {
            return "ENOPERM";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return DatatypeConverter.printBase64Binary(md.digest((this.secret + ref).getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("should never happen");
        }
    }
}
