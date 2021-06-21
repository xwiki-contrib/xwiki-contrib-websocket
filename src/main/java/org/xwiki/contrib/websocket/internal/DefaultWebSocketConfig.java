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
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;

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
    private ConfigurationSource cs;

    @Inject
    private Container container;

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
            String protocol = sslEnabled() ? "wss" : "ws";

            HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
            String host = request.getHeader("host");
            if (host.indexOf(':') != -1) {
                host = host.substring(0, host.indexOf(':'));
            }

            externalPath = String.format("%s://%s:%s/", protocol, host, getPort());
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
