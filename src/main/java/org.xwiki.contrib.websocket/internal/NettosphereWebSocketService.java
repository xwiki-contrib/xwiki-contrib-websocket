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

import java.util.Map;
import java.util.HashMap;
import java.io.StringWriter;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Named;

import org.slf4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.atmosphere.nettosphere.Handler;
import org.atmosphere.nettosphere.Nettosphere;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.websocket.WebSocketEventListener;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.contrib.websocket.WebSocketService;
import org.xwiki.contrib.websocket.WebSocketHandler;
import org.xwiki.component.internal.multi.ComponentManagerManager;

@Component
@Named("nettosphere")
@Singleton
public class NettosphereWebSocketService implements WebSocketService, Handler, Initializable
{
    private int port;
    private String externalPath;

    private Map<String, DocumentReference> userByKey = new HashMap<String, DocumentReference>();
    private Map<DocumentReference, String> keyByUser = new HashMap<DocumentReference, String>();
    private Map<String, NettosphereWebSocket> sockByURI =
        new HashMap<String, NettosphereWebSocket>();

    @Inject
    private ComponentManagerManager compMgrMgr;

    @Inject
    private ConfigurationSource cs;

    @Inject
    private Logger logger;

    @Override
    public String getKey(DocumentReference userRef)
    {
        String key = keyByUser.get(userRef);
        if (key != null) {
            return key;
        }
        key = RandomStringUtils.randomAlphanumeric(20);
        keyByUser.put(userRef, key);
        userByKey.put(key, userRef);
        return key;
    }

    public void initialize()
    {
        String bindTo = cs.getProperty("websocket.bindTo", "0.0.0.0");
        this.externalPath = cs.getProperty("websocket.externalPath", String.class);
        this.port = cs.getProperty("websocket.port", 8093);

        Config.Builder b = new Config.Builder();
        b.resource(this).port(this.port).host(bindTo).build();
        Nettosphere s = new Nettosphere.Builder().config(b.build()).build();
        s.start();
    }

    public String getExternalPath()
    {
        return this.externalPath;
    }

    public int getPort()
    {
        return this.port;
    }

    public void handle(AtmosphereResource r)
    {
        try {
            String key = r.getRequest().getParameter("k");
            DocumentReference user = userByKey.get(key);

            if (user == null || key == null) {
                r.close();
                return;
            }

            String uri = r.getRequest().getRequestURI();
            String handlerName = uri.substring(uri.lastIndexOf('/') + 1);
            String wiki = uri.substring(0,uri.lastIndexOf('/'));
            wiki = wiki.substring(wiki.lastIndexOf('/') + 1);

            // sort of "cannonicalize" the uri into <wiki>/<handler>?<key>
            uri = wiki + "/" + handlerName + "?" + key;

            NettosphereWebSocket sock = this.sockByURI.get(uri);
            if (sock == null || !sock.uuid().equals(r.uuid())) {
                ComponentManager cm = this.compMgrMgr.getComponentManager("wiki:" + wiki, false);
                if (cm == null) {
                    throw new RuntimeException("No ComponentManager could be found for wiki [" +
                                               wiki + "] are you sure it is a real subwiki?");
                }
                WebSocketHandler handler = cm.getInstance(WebSocketHandler.class, handlerName);
                if (handler != null) {
                    sock = new NettosphereWebSocket(user, uri, r, key, wiki);
                    this.sockByURI.put(uri, sock);
                    try {
                        handler.onWebSocketConnect(sock);
                    } catch (Exception e) {
                        logger.warn("Exception in {}.onWebSocketConnect()... [{}]",
                                    handler.getClass().getName(), ExceptionUtils.getStackTrace(e));
                    }

                    final NettosphereWebSocket s = sock;
                    r.addEventListener(new WebSocketEventListenerAdapter() {
                        public void onMessage(WebSocketEventListener.WebSocketEvent event) {
                            //System.out.println("onMessage(" + event.message() + ")");
                            try {
                                s.message(event.message().toString());
                            } catch (Exception e) {
                                logger.warn("Exception in WebSocket.message()... [{}]",
                                            ExceptionUtils.getStackTrace(e));
                            }
                        }
                        public void onDisconnect(WebSocketEventListener.WebSocketEvent event) {
                            s.disconnect();
                            sockByURI.remove(s.getKey() + "/" + s.getPath());
                        }
                    });
                } else {
                    r.getResponse().write("ERROR: no registered component for path [" + uri + "]");
                    r.close();
                }
                return;
            }

        } catch (Exception ohCrap) {
            logger.warn("Something broke when taking a connection [{}]",
                        ExceptionUtils.getStackTrace(ohCrap));
        }
    }
}
