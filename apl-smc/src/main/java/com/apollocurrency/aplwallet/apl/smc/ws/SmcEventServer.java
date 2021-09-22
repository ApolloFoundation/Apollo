/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.events.SmcEvent;
import com.apollocurrency.aplwallet.apl.smc.events.SmcEventType;
import com.apollocurrency.smc.contract.vm.event.SmcContractEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketException;

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
    private static final SmcEventResponse INVALID_RESPONSE;

    private static final ObjectMapper MAPPER;

    static {
        INVALID_RESPONSE = SmcEventResponse.builder()
            .errorCode(2)
            .errorDescription("Wrong request structure.")
            .build();

        MAPPER = new ObjectMapper();
        MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }

    @Getter
    private final ConnectionManager connectionManager;
    private final SmcEventService eventService;

    @Inject
    public SmcEventServer(SmcEventService eventService) {
        this.eventService = eventService;
        this.connectionManager = new ConnectionManager();
    }

    @Override
    public void onClose(SmcEventSocket socket, int code, String message) {
        connectionManager.remove(socket.getContract(), socket.getSession());
    }

    @Override
    public void onOpen(SmcEventSocket socket) {
        if (eventService.isExist(socket.getContract())) {
            connectionManager.register(socket.getContract(), socket.getSession());
        } else {
            throw new WebSocketException("Contract not found, address=" + socket.getContract());
            //socket.getSession().close(StatusCode.UNDEFINED, "Contract not found, address="+socket.getContract());
        }
    }

    @Override
    public void onMessage(SmcEventSocket socket, String message) {
        if (message.toLowerCase(Locale.US).contains("bye")) {
            socket.getSession().close(StatusCode.NORMAL, "Thanks");
        } else {
            try {
                SmcEventResponse response;
                var request = deserializeMessage(message);
                switch (request.getOperation()) {
                    case SUBSCRIBE:
                        //call subscription routine
                        response = SmcEventResponse.builder().build();
                        break;
                    case UNSUBSCRIBE:
                        //call unsubscription routine
                        response = SmcEventResponse.builder().build();
                        break;
                    default:
                        response = INVALID_RESPONSE;
                }
                socket.sendWebSocketText(serializeMessage(response));
            } catch (JsonProcessingException e) {
                socket.sendWebSocketText(serializeMessage(INVALID_RESPONSE));
            }
        }
    }

    public void onSmcEventEmitted(@Observes @SmcEvent(SmcEventType.EMIT_EVENT) SmcContractEvent contractEvent) {
        log.info("Emitted event={}", contractEvent);
    }

    protected SmcEventRequest deserializeMessage(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, SmcEventRequest.class);
    }

    @SneakyThrows
    protected String serializeMessage(SmcEventResponse response) {
        return MAPPER.writeValueAsString(response);
    }
}
