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

import org.xwiki.component.annotation.Role;

@Role
public interface WebSocketConfig
{
    /** Whether SSL should be enabled. */
    boolean sslEnabled();

    /**
     * The filename containing server's certificate in OpenSSL PEM format
     * followed by whatever intermediate certificates are necessary.
     */
    String getCertChainFilename();

    /** The filename of the SSL private key in OpenSSL PEM format. */
    String getPrivateKeyFilename();

    /** The IP address to bind to, in case of a machine with multiple interfaces. */
    String getBindTo();

    /**
     * The external websocket URL to be advertised to clients, the default will be the
     * server's hostname and 'ws' oe 'wss' depending on whether ssl is enabled but in
     * the event that the websocket is tunneled through a different machine or the
     * hostname is not the actual domain, this allows it to be specified manually.
     *
     * Examples include:
     * ws://my.website.com:5678/
     * wss://123.45.67.8:5556/
     */
    String getExternalPath();

    /** The port number to bind machine. */
    int getPort();
}
