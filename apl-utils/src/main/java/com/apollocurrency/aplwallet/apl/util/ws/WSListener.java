/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.ws;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletionStage;

/**
 * Simple web socket listener to work with Apl Supervisor protocol
 *
 * @author alukin@gmail.com
 */
public class WSListener implements Listener {

    @Override
    public CompletionStage<?> onText(WebSocket webSocket,
            CharSequence data, boolean last) {

        System.out.println("onText: " + data);

        return Listener.super.onText(webSocket, data, last);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("onOpen");
        Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
            String reason) {
        System.out.println("onClose: " + statusCode + " " + reason);
        return Listener.super.onClose(webSocket, statusCode, reason);
    }
}
