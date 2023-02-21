/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class OneErrorSocket extends WebSocketAdapter {
    private final String errorMessage;

    public OneErrorSocket(String errorMessage) {
        this.errorMessage = errorMessage;
        log.trace("Created socket, error={}", errorMessage);
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        throw new WebSocketException(errorMessage);
    }

    @SneakyThrows
    @Override
    public void onWebSocketText(String message) {

    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {

    }

    @Override
    public void onWebSocketError(Throwable cause) {
    }

}
