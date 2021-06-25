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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.XWikiServletResponseStub;

/**
 * Initializes the XWiki context for WebSocket handlers.
 * 
 * @version $Id$
 */
@Component(roles = WebSocketContextInitializer.class)
@Singleton
public class WebSocketContextInitializer
{
    @Inject
    private XWikiStubContextProvider contextProvider;

    @Inject
    private Execution execution;

    @Inject
    private Container container;

    /**
     * Initializes the XWiki context based on the provided WebSocket URL.
     * 
     * @param request the
     */
    public void initialize(XWikiWebSocketRequest request)
    {
        ExecutionContext context = this.execution.getContext();
        if (context == null) {
            context = new ExecutionContext();
            this.execution.setContext(context);
        }

        XWikiContext xcontext = this.contextProvider.createStubContext();
        xcontext.setWikiId(request.getWebSocketURI().getWiki());
        xcontext.setRequest(request);
        xcontext.setResponse(new XWikiServletResponseStub());
        xcontext.declareInExecutionContext(context);

        this.container.setRequest(new ServletRequest(request));
        this.container.setResponse(new ServletResponse(xcontext.getResponse()));

        try {
            XWikiUser xwikiUser = xcontext.getWiki().checkAuth(xcontext);
            if (xwikiUser != null) {
                xcontext.setUserReference(xwikiUser.getUserReference());
            }
        } catch (XWikiException e) {
            throw new RuntimeException(
                "Failed to authenticate the user for WebSocket [" + request.getWebSocketURI() + "].", e);
        }
    }
}
