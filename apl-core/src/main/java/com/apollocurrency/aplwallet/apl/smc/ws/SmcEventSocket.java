/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.smc.data.type.Address;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SmcEventSocket extends WebSocketAdapter {
    private final Address contract;
    private final SmcEventSocketListener listener;
    private final HttpServletRequest request;

    public SmcEventSocket(String address, HttpServletRequest request, SmcEventSocketListener listener) {
        this.contract = AplAddress.valueOf(Objects.requireNonNull(address, "contractAddress"));
        this.listener = Objects.requireNonNull(listener, "SmcEventSocketListener");
        this.request = Objects.requireNonNull(request, "request");
        log.trace("Created socket for contract address={}", contract);
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        log.info("Socket Connected: {}", sess);
        listener.onOpen(this);
    }

    @SneakyThrows
    @Override
    public void onWebSocketText(String message) {
        log.info("Received TEXT message: {}", message);
        listener.onMessage(this, message);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        log.info("Socket Closed: [{}] {}", statusCode, reason);
        listener.onClose(this, statusCode, reason);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        log.error("onError: ", cause);
    }

    public Address getContract() {
        return contract;
    }

    public void sendWebSocketText(String message) {
        getRemote().sendStringByFuture(message);
    }
}
