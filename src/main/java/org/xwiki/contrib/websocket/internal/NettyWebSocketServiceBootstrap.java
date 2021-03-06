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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Triggers the initialization of the Netty-based WebSocket service.
 * 
 * @version $Id$
 */
@Component
@Named(NettyWebSocketServiceBootstrap.NAME)
@Singleton
public class NettyWebSocketServiceBootstrap extends AbstractEventListener
{
    /**
     * The name used to register this event listener.
     */
    public static final String NAME = "";

    @Inject
    @SuppressWarnings("unused")
    private NettyWebSocketService webSocketService;

    /**
     * Default constructor.
     */
    public NettyWebSocketServiceBootstrap()
    {
        // We don't listen to any events. We just want to trigger the initialization of the WebSocket service when this
        // event listener component is instantiated by the component manager.
        super(NAME, Collections.emptyList());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Nothing to do here because we're not listening to any event.
    }
}
