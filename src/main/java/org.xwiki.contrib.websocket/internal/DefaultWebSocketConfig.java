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
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.component.annotation.Component;

@Component
public class DefaultWebSocketConfig implements WebSocketConfig
{
    @Inject
    private ConfigurationSource cs;

    @Override
    public boolean sslEnabled()
    {
        return this.cs.getProperty("websocket.ssl.enable", true);
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
        return this.cs.getProperty("websocket.externalPath", String.class);
    }

    @Override
    public int getPort()
    {
        return this.cs.getProperty("websocket.port", 8093);
    }
}
