/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractEventService;
import com.apollocurrency.aplwallet.apl.smc.event.SmcEvent;
import com.apollocurrency.aplwallet.apl.smc.event.SmcEventType;
import com.apollocurrency.smc.contract.vm.event.SmcContractEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.StatusCode;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class SmcEventServer implements SmcEventSocketListener {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConnectionManager connectionManager;
    private final SmcContractEventService eventService;

    @Inject
    public SmcEventServer(SmcContractEventService eventService) {
        this.eventService = eventService;
        this.connectionManager = new ConnectionManager();
    }

    @Override
    public void onClose(SmcEventSocket socket, int code, String message) {
        connectionManager.remove(socket.getContract(), socket.getSession());
    }

    @Override
    public void onOpen(SmcEventSocket socket) {
        connectionManager.register(socket.getContract(), socket.getSession());
    }

    @Override
    public void onMessage(SmcEventSocket socket, String message) {
        if (message.toLowerCase(Locale.US).contains("bye")) {
            socket.getSession().close(StatusCode.NORMAL, "Thanks");
        } else {
            socket.sendWebSocketText("Hi " + socket.getRemote().getInetSocketAddress().toString());
        }
    }

    public void onSmcEventEmitted(@Observes @SmcEvent(SmcEventType.EMIT_EVENT) SmcContractEvent contractEvent) {
        log.info("Emitted event={}", contractEvent);
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @SneakyThrows
    protected Message deserializeMessage(String json) {
        return MAPPER.readValue(json, Message.class);
    }

    @SneakyThrows
    protected String serializeMessage(Message msg) {
        return MAPPER.writeValueAsString(msg);
    }

    public void broadcast(Message m) {
        //connectionManager.broadcast(m);
    }
}
