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

import java.net.URI;
import java.net.URISyntaxException;

import org.xwiki.contrib.websocket.WebSocketHandler;

/**
 * The URI used to access a {@link WebSocketHandler}.
 * 
 * @version $Id$
 */
public class WebSocketURI
{
    private static final String SEPARATOR = "/";

    private final String baseURI;

    private final String wiki;

    private final String handler;

    /**
     * Parse the given WebSocket URI, which can be absolute or relative (e.g. just the path)
     * 
     * @param webSocketURI the URI to parse
     * @throws URISyntaxException if the given URI is not valid or it doesn't contain all the expected information
     */
    public WebSocketURI(String webSocketURI) throws URISyntaxException
    {
        URI uri = new URI(webSocketURI);
        String path = uri.getPath();
        if (path == null) {
            path = "";
        }
        String[] pathSegments = path.split(SEPARATOR);
        if (pathSegments.length >= 2) {
            this.wiki = pathSegments[pathSegments.length - 2];
            this.handler = pathSegments[pathSegments.length - 1];
            this.baseURI = webSocketURI.substring(0, webSocketURI.lastIndexOf(wiki + SEPARATOR + handler));
        } else {
            throw new URISyntaxException(webSocketURI,
                "Invalid WebSocket URI: the path is missing the wiki and the WebSocket handler.");
        }
    }

    /**
     * Creates a new WebSocket URI with the given components.
     * 
     * @param baseURI the base URI (usually taken from the configuration)
     * @param wiki the wiki where the {@link WebSocketHandler} component is registered
     * @param handler the hint of the {@link WebSocketHandler} component implementation targeted by this URI
     */
    public WebSocketURI(String baseURI, String wiki, String handler)
    {
        this.baseURI = baseURI;
        this.wiki = wiki;
        this.handler = handler;
    }

    @Override
    public String toString()
    {
        return this.baseURI + this.wiki + SEPARATOR + this.handler;
    }

    /**
     * @return the wiki where the {@link WebSocketHandler} component is registered
     */
    public String getWiki()
    {
        return wiki;
    }

    /**
     * @return the hint of the {@link WebSocketHandler} component implementation targeted by this URI
     */
    public String getHandler()
    {
        return handler;
    }
}
