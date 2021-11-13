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
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.websocket.WebSocketConfig;

import com.xpn.xwiki.XWikiContext;

/**
 * Default {@link WebSocketConfig} implementation.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWebSocketConfig implements WebSocketConfig
{
    @Inject
    private Logger logger;

    @Inject
    private ConfigurationSource cs;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public boolean sslEnabled()
    {
        return this.cs.getProperty("websocket.ssl.enable", false);
    }

    @Override
    public String getCertChainFilename()
    {
        return this.cs.getProperty("websocket.ssl.certChainFile", String.class);
    }

    @Override
    public String getPrivateKeyFilename()
    {
        return this.cs.getProperty("websocket.ssl.pkcs8PrivateKeyFile", String.class);
    }

    @Override
    public String getBindTo()
    {
        return this.cs.getProperty("websocket.bindTo", "0.0.0.0");
    }

    @Override
    public String getExternalPath()
    {
        String externalPath = this.cs.getProperty("websocket.externalPath", String.class);
        if (externalPath == null) {
            try {
                String scheme = sslEnabled() ? "wss" : "ws";

                XWikiContext xcontext = this.xcontextProvider.get();
                URL serverURL = xcontext.getURLFactory().getServerURL(xcontext);
                String path = '/' + xcontext.getWiki().getWebAppPath(xcontext);

                // We have to add the path afterwards because the URI constructor double encodes it.
                // See https://bugs.openjdk.java.net/browse/JDK-8151244 (URI Constructor Doesn't Encode Path Correctly)
                externalPath =
                    new URI(scheme, null, serverURL.getHost(), getPort(), null, null, null).toString() + path;
            } catch (Exception e) {
                this.logger.warn("Failed to create WebSocket base URI. Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        } else if (!externalPath.endsWith("/")) {
            externalPath += '/';
        }

        return externalPath;
    }

    @Override
    public int getPort()
    {
        return this.cs.getProperty("websocket.port", 8093);
    }

    @Override
    public int maxFrameSize()
    {
        return this.cs.getProperty("websocket.maxFrameSize", 20000000);
    }
}
