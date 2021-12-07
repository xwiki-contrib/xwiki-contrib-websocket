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

import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWebSocketConfig}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultWebSocketConfigTest
{
    @InjectMockComponents
    private DefaultWebSocketConfig config;

    @MockComponent
    private ConfigurationSource cs;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiURLFactory urlFactory;

    @BeforeEach
    void configure()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.xcontext.getURLFactory()).thenReturn(this.urlFactory);

        when(this.cs.getProperty("websocket.ssl.enable", false)).thenReturn(false);
        when(this.cs.getProperty("websocket.port", 8093)).thenReturn(8093);
    }

    @Test
    void getDefaultExternalPath() throws Exception
    {
        when(this.urlFactory.getServerURL(this.xcontext)).thenReturn(new URL("http://www.xwik.org"));
        when(this.xwiki.getWebAppPath(this.xcontext)).thenReturn("xwiki/");

        assertEquals("ws://www.xwik.org:8093/xwiki/", this.config.getExternalPath());
    }

    @Test
    void getDefaultExternalPathWithSSLAndRoot() throws Exception
    {
        when(this.cs.getProperty("websocket.ssl.enable", false)).thenReturn(true);
        when(this.cs.getProperty("websocket.port", 8093)).thenReturn(8094);

        when(this.urlFactory.getServerURL(this.xcontext)).thenReturn(new URL("http://www.xwik.com"));
        when(this.xwiki.getWebAppPath(this.xcontext)).thenReturn("");

        assertEquals("wss://www.xwik.com:8094/", this.config.getExternalPath());
    }
}
