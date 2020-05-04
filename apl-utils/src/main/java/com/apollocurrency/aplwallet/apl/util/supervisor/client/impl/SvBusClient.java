/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.client.impl;

import com.apollocurrency.aplwallet.apl.util.supervisor.client.ConnectionStatus;
import com.apollocurrency.aplwallet.apl.util.supervisor.client.MessageDispatcher;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusError;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusErrorCodes;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvChannelHeader;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvChannelMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Client of Apollo Supervisor or similar service with the same protocol.
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class SvBusClient implements Listener, Closeable {

    private ConnectionStatus state = ConnectionStatus.NOT_CONNECTD;
    private WebSocket webSocket;
    @Getter
    private final URI serverURI;
    private final HttpClient httpClient;

    private PathParamProcessor pathProcessor;
    private final MessageDispatcherImpl dispatcher;

    public SvBusClient(URI serverURI, MessageDispatcher dispatcher) {
        this.pathProcessor = new PathParamProcessor();
        this.dispatcher = (MessageDispatcherImpl) dispatcher;
        this.serverURI = serverURI;
        httpClient = HttpClient.newHttpClient();

    }

    public boolean isConnected() {
        return state == ConnectionStatus.CONNECTED;
    }

    public ConnectionStatus getState() {
        return state;
    }

    private void setState(ConnectionStatus cs) {
        state = cs;
    }

    /**
     * Connect to given by constructor URI
     *
     * @return true if connected
     */
    public boolean connect() {
        boolean res;
        setState(ConnectionStatus.CONNECTING);
        try {
            webSocket = httpClient.newWebSocketBuilder()
                    .buildAsync(serverURI, this)
                    .join();
            res = !webSocket.isInputClosed();
            if (res) {
                dispatcher.onConnectionUp(serverURI);
            }
        } catch (CompletionException ex) {
            setState(ConnectionStatus.ERROR);
            log.info("Can not connect to {}", serverURI);
            res = false;
        }
        return res;
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public boolean sendMessage(SvChannelMessage envelope) throws JsonProcessingException {
        boolean res = false;
        //TODO: binary mode with binary header
        envelope.header.from_timestamp = new Date();
        ObjectMapper mapper = pathProcessor.getMapper();
        String msg = mapper.writeValueAsString(envelope);
        if (webSocket != null) {
            webSocket.sendText(msg, true);
            //TODO may be join() ?
            res = true;
        } else {
            log.error("Websocket is not connected error");
        }
        return res;
    }

    public void sendError(int code, String message, SvChannelHeader rqheader) {
        SvBusResponse resp = new SvBusResponse();
        resp.error = new SvBusError();
        resp.error.errorCode = code;
        resp.error.descritption = message;
        SvChannelHeader hdr = new SvChannelHeader();
        hdr.from = dispatcher.getMyAddress().toString();
        if (rqheader != null) {
            hdr.to = rqheader.from;
            hdr.path = rqheader.path;
            hdr.inResponseTo = hdr.messageId;
        } else {
            hdr.to = serverURI.toString();
            hdr.path = MessageDispatcher.ERROR_PATH;
            hdr.inResponseTo = 0L;
        }
        hdr.from_timestamp = new Date();
        hdr.messageId = dispatcher.nextMessageId();
        SvChannelMessage msg = new SvChannelMessage(hdr, resp);
        try { //trying my besr, no result check
            sendMessage(msg);
        } catch (JsonProcessingException ex) {
        }
    }

// should be common for binary and text mode
    private void processIncoming(CharSequence data, boolean bin) {
        try {
            ObjectMapper mapper = pathProcessor.getMapper();
            JsonNode env_node = mapper.readTree(data.toString());
            JsonNode header_node = env_node.findValue("header");
            if (header_node == null) {
                sendError(SvBusErrorCodes.INVALID_HEADER, "no header node in JSON", null);
                log.error("No header in incoming message");
                return;
            }
            SvChannelHeader header = mapper.treeToValue(header_node, SvChannelHeader.class);
            if (header == null) {
                log.error("Can not parse header. Message: {}", data);
                sendError(SvBusErrorCodes.INVALID_HEADER, "Invalid header node in JSON", null);
                return;
            }
            JsonNode body = env_node.findValue("body"); // same as above, i think that better to add validation for npes to guarantee message correct format
            if (body == null) {
                log.warn("No body node in JSON");
            }
            dispatcher.handleIncoming(header, body);
        } catch (JsonProcessingException ex) {
            sendError(SvBusErrorCodes.INVALID_MESSAGE, "Invalud JSON message: " + ex.getMessage(), null);
            log.error("JSON processing error. Message: {}", data, ex);
        }
    }

    //TODO: implement
    private SvChannelMessage decodeBinary(ByteBuffer data) {
        log.error("Binary mode is not implemented yet");
        return null;
    }

    //TODO: implement
    private ByteBuffer encodeBinary(SvChannelMessage data) {
        log.error("Binary mode is not implemented yet");
        return null;
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

        log.debug("onText: {}", data);
        processIncoming(data, false);
        return Listener.super.onText(webSocket, data, last);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        setState(ConnectionStatus.CONNECTED);
        Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
            String reason) {
        log.debug("onClose: {} {}", statusCode, reason);
        setState(ConnectionStatus.DISCONNECTED);
        return Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        setState(state.ERROR); //TODO maybe save or print error stacktrace
        Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        log.debug("onPong: ");
        return Listener.super.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        log.debug("onPing: ");
        return Listener.super.onPing(webSocket, message);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {

        //   processIncoming(webSocket, decodeBinary(data), true);
        return Listener.super.onBinary(webSocket, data, last);
    }

    @Override
    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok");
        }
    }

    void sendText(CharSequence value) {
        webSocket.sendText(value, true);
    }
}
