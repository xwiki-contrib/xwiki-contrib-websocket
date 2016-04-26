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

@Role
public interface WebSocketService
{
    /**
     * Get a token for connecting to the websocket based on the user's identity.
     */
    String getKey(String wiki, DocumentReference userRef);

    /**
     * @return the user matching that given token for the given wiki, or null if token is not valid.
     */
    DocumentReference getUser(String wiki, String key);
}
