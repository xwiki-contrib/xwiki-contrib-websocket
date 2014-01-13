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
package org.xwiki.websocket.internal;

import java.util.Map;
import java.util.HashMap;
import java.io.StringWriter;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.atmosphere.nettosphere.Handler;
import org.atmosphere.nettosphere.Nettosphere;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.websocket.WebSocketService;
import org.xwiki.websocket.WebSocketHandler;

@Component
@Named("nettosphere")
public class NettosphereWebSocketService implements WebSocketService, Handler, Initializable
{
    private int port;
    private String externalHost;

    private Map<String, DocumentReference> userByKey = new HashMap<String, DocumentReference>();
    private Map<DocumentReference, String> keyByUser = new HashMap<DocumentReference, String>();
    private Map<String, NettosphereWebSocket> sockByKeyAndURI =
        new HashMap<String, NettosphereWebSocket>();

    @Inject
    private ComponentManager compMgr;

    @Inject
    private ConfigurationSource cs;

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
        Config.Builder b = new Config.Builder();

        String bindTo = cs.getProperty("websocket.bindTo", "0.0.0.0");
        this.externalHost = cs.getProperty("websocket.externalHost", String.class);
        this.port = cs.getProperty("websocket.port", 8093);

        b.resource(this).port(this.port).host(bindTo).build();
        Nettosphere s = new Nettosphere.Builder().config(b.build()).build();
        s.start();
    }

    public String getExternalHost()
    {
        return this.externalHost;
    }

    public int getPort()
    {
        return this.port;
    }

    public void handle(AtmosphereResource r)
    {
        String key = r.getRequest().getParameter("k");
        DocumentReference user = userByKey.get(key);
        try {
            if (user == null || key == null) {
                r.close();
                return;
            }

            // strip leading /
            String uri = r.getRequest().getRequestURI().substring(1);

            NettosphereWebSocket sock = this.sockByKeyAndURI.get(key + uri);
            if (sock == null || !sock.uuid().equals(r.uuid())) {
                WebSocketHandler handler = this.compMgr.getInstance(WebSocketHandler.class, uri);
                if (handler != null) {
                    sock = new NettosphereWebSocket(user, uri, r, key);
                    this.sockByKeyAndURI.put(key + uri, sock);
                    handler.onWebSocketConnect(sock);
                    /* TODO: disconnects are not fired by nettosphere
                    final NettosphereWebSocket s = sock;
                    r.addEventListener(new AtmosphereResourceEventListenerAdapter () {
                        public void onDisconnect(AtmosphereResourceEvent event) {
                            s.disconnect();
                            sockByKeyAndURI.remove(s.getKey() + s.getPath());
                        }
                    });*/
                } else {
                    r.getResponse().write("ERROR: no registered component for path [" + uri + "]");
                    r.close();
                }
                return;
            }

            String content = "";
            StringWriter writer = new StringWriter();
            IOUtils.copy(r.getRequest().getReader(), writer);
            content = writer.toString();

            //System.out.println(uri + " got content! " + content);

            sock.message(content);

        } catch (Exception iDoNotCare) { }
    }
}
