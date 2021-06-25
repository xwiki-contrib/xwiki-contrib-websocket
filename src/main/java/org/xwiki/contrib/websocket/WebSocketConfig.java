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
package org.xwiki.contrib.websocket;

import org.xwiki.component.annotation.Role;

/**
 * WebSocket configuration.
 * 
 * @version $Id$
 */
@Role
public interface WebSocketConfig
{
    /**
     * @return whether SSL should be enabled
     */
    boolean sslEnabled();

    /**
     * @return the name of the file containing server's certificate in OpenSSL PEM format followed by whatever
     *         intermediate certificates are necessary
     */
    String getCertChainFilename();

    /**
     * @return the name of the file that holds the SSL private key in OpenSSL PEM format
     */
    String getPrivateKeyFilename();

    /**
     * @return the IP address to bind to, useful when there are multiple network interfaces
     */
    String getBindTo();

    /**
     * @return the external WebSocket URL to be advertised to clients; the default will be the server's host name and
     *         'ws' or 'wss' depending on whether SSL is enabled but in the event that the WebSocket is tunneled through
     *         a different machine or the host name is not the actual domain, this allows it to be specified manually;
     *         examples include: ws://my.website.com:5678/ wss://123.45.67.8:5556/
     */
    String getExternalPath();

    /**
     * @return the port number the WebSocket service should listen to
     */
    int getPort();

    /**
     * @return the maximum size of a frame sent to the WebSocket; keep this small to prevent DoS but bigger than the
     *         biggest real-time document you will edit; default is 20MB
     */
    int maxFrameSize();
}
