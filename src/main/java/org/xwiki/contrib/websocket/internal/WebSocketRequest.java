package org.xwiki.contrib.websocket.internal;

import java.util.List;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * Please comment here
 *
 * @version $Id$
 */
public interface WebSocketRequest
{
    boolean isValid();

    String getKey();

    String getHandlerName();

    String getWiki();

    DocumentReference getUser();

    Map<String, List<String>> getParameters();
}
