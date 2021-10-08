/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventResponse;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import com.apollocurrency.smc.data.type.Address;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.util.Locale;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SmcEventSocket extends WebSocketAdapter {
    private static final String INVALID_REQUEST_FORMAT_RESPONSE;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        INVALID_REQUEST_FORMAT_RESPONSE = serializeMessage(SmcErrorReceipt.error(SmcEventServerErrors.WRONG_REQUEST_STRUCTURE));
    }

    private final Address contract;
    private final SmcEventSocketListener listener;

    public SmcEventSocket(String address, SmcEventSocketListener listener) {
        this.contract = AplAddress.valueOf(Objects.requireNonNull(address, "contractAddress"));
        this.listener = Objects.requireNonNull(listener, "SmcEventSocketListener");
        log.trace("Created socket for contract address={}", contract);
    }

    public SmcEventSocket(SmcEventSocket copy) {
        this.contract = copy.contract;
        this.listener = copy.listener;
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
        log.debug("Received TEXT message: {}", message);
        if (message.toLowerCase(Locale.US).equals("exit")) {
            getSession().close(StatusCode.NORMAL, "Thanks");
        } else {
            try {
                var request = deserializeMessage(message);
                if (request.getEvents().isEmpty()) {
                    sendWebSocketText(serializeMessage(SmcErrorReceipt.error(request.getRequestId(),
                        SmcEventServerErrors.INVALID_REQUEST_ARGUMENTS)));
                } else {
                    listener.onMessage(this, request);
                }
            } catch (JsonProcessingException e) {
                sendWebSocketText(INVALID_REQUEST_FORMAT_RESPONSE);
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        log.debug("Socket Closed: [{}] {}", statusCode, reason);
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

    protected static SmcEventSubscriptionRequest deserializeMessage(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, SmcEventSubscriptionRequest.class);
    }

    @SneakyThrows
    protected static String serializeMessage(Object response) {
        return MAPPER.writeValueAsString(response);
    }

    public void sendWebSocket(SmcEventResponse response) {
        sendWebSocketText(serializeMessage(response));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SmcEventSocket that = (SmcEventSocket) o;
        return contract.equals(that.contract) && getSession().equals(that.getSession()) && getRemote().equals(that.getRemote());
    }

    @Override
    public int hashCode() {
        return Objects.hash(contract, getSession(), getRemote());
    }
}
