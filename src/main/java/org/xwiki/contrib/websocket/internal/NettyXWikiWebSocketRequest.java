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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

/**
 * Adapts a {@link FullHttpRequest} to {@link XWikiRequest}.
 * 
 * @version $Id$
 */
public class NettyXWikiWebSocketRequest extends XWikiServletRequestStub implements XWikiWebSocketRequest
{
    private final HttpRequest request;

    private final Channel channel;

    private final WebSocketURI webSocketURI;

    private final Map<String, Cookie> cookies;

    private final HttpSession session = new HttpSessionStub();

    /**
     * Creates a new XWiki request that wraps the given WebSocket request.
     * 
     * @param request the WebSocket request to wrap
     * @param channel the WebSocket communication channel
     * @throws URISyntaxException if the WebSocket request URI is not valid or it doesn't contain the expected
     *             information
     */
    public NettyXWikiWebSocketRequest(HttpRequest request, Channel channel) throws URISyntaxException
    {
        super(null, decodeParameters(request.uri()));

        this.request = request;
        this.channel = channel;
        this.webSocketURI = new WebSocketURI(request.uri());
        this.cookies = parseCookies();
    }

    @Override
    public WebSocketURI getWebSocketURI()
    {
        return this.webSocketURI;
    }

    @Override
    public String getHeader(String name)
    {
        return this.request.headers().get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name)
    {
        return Collections.enumeration(this.request.headers().getAll(name));
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return Collections.enumeration(this.request.headers().names());
    }

    @Override
    public long getDateHeader(String name)
    {
        return this.request.headers().getTimeMillis(name);
    }

    @Override
    public int getIntHeader(String name)
    {
        return this.request.headers().getInt(name);
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return this.cookies.get(cookieName);
    }

    @Override
    public Cookie[] getCookies()
    {
        return this.cookies.values().stream().toArray(Cookie[]::new);
    }

    private Map<String, Cookie> parseCookies()
    {
        String cookieString = this.request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            return ServerCookieDecoder.LAX.decode(cookieString).stream()
                .map(nettyCookie -> new Cookie(nettyCookie.name(), nettyCookie.value()))
                .collect(Collectors.toMap(Cookie::getName, Function.identity()));
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public String getMethod()
    {
        return this.request.method().name();
    }

    @Override
    public String getRequestURI()
    {
        return this.request.uri();
    }

    @Override
    public String getScheme()
    {
        return this.request.protocolVersion().protocolName();
    }

    @Override
    public String getProtocol()
    {
        return this.request.protocolVersion().text();
    }

    @Override
    public int getContentLength()
    {
        return getIntHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
    }

    @Override
    public String getContentType()
    {
        return getHeader(HttpHeaderNames.CONTENT_TYPE.toString());
    }

    private static Map<String, String[]> decodeParameters(String uri)
    {
        Map<String, String[]> parameters = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : new QueryStringDecoder(uri).parameters().entrySet()) {
            parameters.put(entry.getKey(), entry.getValue().toArray(new String[] {}));
        }
        return parameters;
    }

    @Override
    public HttpSession getSession()
    {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create)
    {
        return this.session;
    }

    @Override
    public String getRemoteAddr()
    {
        return ((InetSocketAddress) this.channel.remoteAddress()).getAddress().getHostAddress();
    }

    @Override
    public String getRemoteHost()
    {
        return ((InetSocketAddress) this.channel.remoteAddress()).getHostName();
    }

    @Override
    public int getRemotePort()
    {
        return ((InetSocketAddress) this.channel.remoteAddress()).getPort();
    }

    @Override
    public String getLocalName()
    {
        return ((InetSocketAddress) this.channel.localAddress()).getHostName();
    }

    @Override
    public String getLocalAddr()
    {
        return ((InetSocketAddress) this.channel.localAddress()).getAddress().getHostAddress();
    }

    @Override
    public int getLocalPort()
    {
        return ((InetSocketAddress) this.channel.localAddress()).getPort();
    }

    @Override
    public String getServerName()
    {
        return getLocalName();
    }

    @Override
    public int getServerPort()
    {
        return getLocalPort();
    }

    @Override
    public String getServletPath()
    {
        return "";
    }

    @Override
    public String getPathInfo()
    {
        try {
            return new URI(this.request.uri()).getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
