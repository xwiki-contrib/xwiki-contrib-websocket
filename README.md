# XWiki WebSocket

[![XWiki labs logo](https://raw.githubusercontent.com/xwiki-labs/xwiki-labs-logo/master/projects/xwikilabs/xwikilabsproject.png "XWiki labs")](https://labs.xwiki.com/xwiki/bin/view/Projects/XWikiLabsProject)

Allows you to create WebSocket enabled Applications in XWiki.

## How it works

You create a component in XWiki which implements **WebSocketHandler** and you use *@Named* to
specify a name for it, then when a client connects to the XWiki WebSocket passing the name of
your component as the path, your component will be called.

## Example

A simple example is the *EchoWebSocketHandler* which simply replies to each WebSocket message
with the same message. Because the *EchoWebSocketHandler* is named `echo`, WebSockets directed to
the `/echo` path will reach this handler.

```java
@Component
@Named("echo")
public class EchoWebSocketHandler implements WebSocketHandler
{
    public void onWebSocketConnect(WebSocket sock)
    {
        sock.onMessage(new WebSocket.Callback() {
            public void call(WebSocket sock)
            {
                String msg = sock.recv();
                sock.send(msg);
            }
        });
    }
}
```

### Accessing the handler

To get a WebSocket URL for accessing the `echo` server, simply use the provided ScriptService
in Velocity as follows:

```
{{velocity}}
{{html clean=false}}
<script>
    // this is a valid WebSocket URL
    var WEBSOCKET_URL = "$services.websocket.getURL('echo')";
</script>
{{/html}}
{{/velocity}}
```

Then to open a socket to this URL, use the WebSocket API in the browser as normal.

```javascript
var ws = new WebSocket(WEBSOCKET_URL);
ws.onopen = function () {
    console.log("Websocket Opened");
    ws.send("Hello World!");
};
ws.onmessage = function (msg) {
    console.log(msg.data);
    if (msg.data === 'Hello World!') {
        console.log("Yay it worked!");
    }
};
```

Read the code of the [EchoWebSocketHandler](https://github.com/xwiki-contrib/xwiki-contrib-websocket/tree/master/xwiki-contrib-websocket-nettosphere/src/main/java/org.xwiki.websocket/EchoWebSocketHandler.java)
for yourself.

## Bugs

* **onDisconnect** messages are not emitted when a client disconnects. This is because
Nettosphere does not emit them and I was unable to reflect the underlying socket. (fixed in 1.6)

## Releasing with Maven
May the force be with you and good luck.

    mvn org.apache.maven.plugins:maven-release-plugin:2.5:prepare -Pintegration-tests -Darguments="-DskipTests -Dxwiki.enforcer.skip=true" -DskipTests
    mvn org.apache.maven.plugins:maven-release-plugin:2.5:perform -Pintegration-tests -Darguments="-DskipTests -Dxwiki.enforcer.skip=true" -DskipTests
