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
import org.xwiki.model.reference.DocumentReference;

/**
 * The services that implements the WebSocket communication.
 * 
 * @version $Id$
 */
@Role
public interface WebSocketService
{
    /**
     * Get a token for connecting to the WebSocket based on the user's identity.
     * 
     * @param wiki the wiki to connect to
     * @param userRef the user for which to retrieve the connection key
     * @return the connection key for the specified user to the specified wiki
     */
    String getKey(String wiki, DocumentReference userRef);

    /**
     * @param wiki the wiki for which the connection key was generated
     * @param key a connection key
     * @return the user matching the given token for the given wiki, or {@code null} if the token is not valid.
     */
    DocumentReference getUser(String wiki, String key);
}
