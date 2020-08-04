/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.ws;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;

/**
 * JDK-11 and above WebSocket client without any external dependencies
 *
 * @author alukin@gmail.com
 */
public class WSClient implements Closeable {

    private Listener wsListener;
    private HttpClient client;
    private WebSocket webSocket;

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public WSClient(Listener wsListener) {
        this.wsListener = wsListener;
        client = HttpClient.newHttpClient();
    }

    /**
     * Connect to given URI
     *
     * @param uri Example: "wss://localhost:8443/wsEndpoint"
     * @return true if connected
     */
    public boolean connect(String uri) {
        boolean res = false;
        webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(uri), wsListener)
                .join();
        return !webSocket.isInputClosed();
    }

    public void sendText(String text) {
        webSocket.sendText(text, true);
    }

    @Override
    public void close() {
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok");
    }
}
