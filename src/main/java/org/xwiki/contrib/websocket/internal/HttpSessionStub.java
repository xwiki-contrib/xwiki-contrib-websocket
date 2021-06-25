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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * Simulates the HTTP (servlet) session for a WebSocket connection, in order to be able to authenticate the XWiki user.
 * 
 * @version $Id$
 */
public class HttpSessionStub implements HttpSession
{
    private int maxInactiveInterval;

    private Map<String, Object> attributes = new HashMap<>();

    @Override
    public long getCreationTime()
    {
        return 0;
    }

    @Override
    public String getId()
    {
        return null;
    }

    @Override
    public long getLastAccessedTime()
    {
        return 0;
    }

    @Override
    public ServletContext getServletContext()
    {
        return null;
    }

    @Override
    public void setMaxInactiveInterval(int interval)
    {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval()
    {
        return this.maxInactiveInterval;
    }

    @Override
    public HttpSessionContext getSessionContext()
    {
        return null;
    }

    @Override
    public Object getAttribute(String name)
    {
        return this.attributes.get(name);
    }

    @Override
    public Object getValue(String name)
    {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return Collections.enumeration(this.attributes.keySet());
    }

    @Override
    public String[] getValueNames()
    {
        return Collections.list(getAttributeNames()).toArray(new String[] {});
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        this.attributes.put(name, value);
    }

    @Override
    public void putValue(String name, Object value)
    {
        this.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name)
    {
        this.attributes.remove(name);
    }

    @Override
    public void removeValue(String name)
    {
        this.removeAttribute(name);
    }

    @Override
    public void invalidate()
    {
        this.attributes.clear();
    }

    @Override
    public boolean isNew()
    {
        return false;
    }
}
