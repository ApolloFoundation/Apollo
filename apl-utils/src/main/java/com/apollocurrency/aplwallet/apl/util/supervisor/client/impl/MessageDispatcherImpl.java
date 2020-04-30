/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.client.impl;

import com.apollocurrency.aplwallet.apl.util.supervisor.client.MessageDispatcher;
import com.apollocurrency.aplwallet.apl.util.supervisor.client.SvRequestHandler;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusError;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusErrorCodes;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusHello;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusMessage;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusRequest;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvBusResponse;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvChannelHeader;
import com.apollocurrency.aplwallet.apl.util.supervisor.msg.SvChannelMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;

/**
 * Implementation of MessageDispoatcher. Actually it is the core of messaging
 * system that performs message passing from remote side to processing routines
 * and back
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class MessageDispatcherImpl implements MessageDispatcher {

    public static long RESPONSE_WAIT_TIMEOUT_MS = 500L; // TODO need to make it configurable

    private final SvConnections connections;
    private final PathParamProcessor pathMatcher;
    private final Deque<SvChannelMessage> outgoingQueue = new LinkedList<>();
    private SvBusHello hello;
    @Getter
    private URI myAddress = null;
    //TODO
    // Which thread pool to use here with response wait latches?
    ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, ResponseLatch> waiting = new ConcurrentHashMap<>();

    public MessageDispatcherImpl() {
        pathMatcher = new PathParamProcessor();
        connections = new SvConnections(this);
    }

    public Map<URI, SvBusClient> getConnections() {
        return connections.getAll();
    }

    /**
     *
     * @return request handler that matches path specification
     */
    private HandlerRecord findRqByPath(String path) {
        HandlerRecord handlerRec = pathMatcher.findAndParse(path);
        return handlerRec;
    }

    @Override
    public void registerRqHandler(String pathSpec, Class<? extends SvBusMessage> rqMapping, Class<? extends SvBusMessage> respMapping, SvRequestHandler handler) {
        pathMatcher.registerRqHandler(pathSpec, rqMapping, respMapping, handler);
    }

    @Override
    public void unregisterRqHandler(String pathSpec) {
        pathMatcher.remove(pathSpec);
    }

    long nextMessageId() {
        return Math.round(Math.random() * (Long.MAX_VALUE - 1));
    }

    boolean isMyAddress(String to) {
        return myAddress.equals(to);
    }

    public SvBusResponse errornousRequestsHandler(JsonNode rqBody, SvChannelHeader rqHeader, int code, String errorInfo) {
        SvBusResponse resp = new SvBusResponse();
        resp.error = new SvBusError();
        resp.error.error_code = code;
        resp.error.descritption = errorInfo + " Request path: " + rqHeader.path;
        return resp;
    }

    void addConnection(URI uri, boolean isDefault) {
        SvBusClient client = new SvBusClient(uri, this);
        connections.put(uri, client, isDefault);
        client.connect();
    }

    private SvBusResponse getResponse(Long rqId) throws SocketTimeoutException {
        SvBusResponse res = null;
        ResponseLatch l = waiting.get(rqId);
        if (l != null) {
            res = l.get(RESPONSE_WAIT_TIMEOUT_MS);
        }
        return res;
    }

    public SvBusResponse sendSync(SvBusMessage rq, String path, URI addr) {
        SvBusResponse res = null;
        SvBusClient client = connections.get(addr);
        if (client == null) {
            client = connections.getDefault().getValue();
        }
        SvChannelHeader header = new SvChannelHeader();
        header.from = myAddress.toString();
        header.to = addr.toString();
        header.path = path;
        header.messageId = nextMessageId();
        SvChannelMessage env = new SvChannelMessage();
        env.header = header;
        env.body = rq;
        try {
            if (!client.sendMessage(env)) {
                outgoingQueue.addLast(env);
            }
            waiting.put(header.messageId, new ResponseLatch());
            res = getResponse(header.messageId);

        } catch (SocketTimeoutException ex) {
            log.warn("Responce wait timeout", ex);
        } catch (JsonProcessingException e) {
            log.error("Can not map response to JSON");
        }
        return res;
    }

    public CompletableFuture<SvBusResponse> sendAsync(SvBusRequest rq, String path, URI addr) {
        CompletableFuture<SvBusResponse> res = CompletableFuture.supplyAsync(() -> sendSync(rq, path, addr), executor);
        return res;
    }

    void sendHello(URI addr) {
        sendSync(hello, "/hello", addr);
    }

    void replyTo(SvChannelHeader rqHeader, SvBusMessage m) {
        SvChannelHeader header = new SvChannelHeader();
        header.from = myAddress.toString();
        header.to = rqHeader.from;
        //TODO: etire path or prefix only without path parameters?
        header.path = rqHeader.path;
        header.inResponseTo = rqHeader.messageId;
        header.messageId = nextMessageId();
        SvBusClient client = connections.get(URI.create(header.to));
        if (client == null) {
            client = connections.getDefault().getValue();
        }
        SvChannelMessage env = new SvChannelMessage();
        env.body = m;
        env.header = header;
        try {
            if (!client.sendMessage(env)) {
                outgoingQueue.addLast(env);
            }
        } catch (JsonProcessingException ex) {
            log.error("Can not map response to JSON");
        }
    }

    void handleResponse(JsonNode body, SvChannelHeader header) {
        ResponseLatch rl = waiting.get(header.inResponseTo);
        if (rl == null) {
            log.warn("Got responce that is not in waiting map. Header: {}", header);
        } else {
            HandlerRecord hr = pathMatcher.find(header.path);
            if (hr == null) { // Maybe set here error response to unlock latch?
                if (header.path.equals(MessageDispatcher.ERROR_PATH)) {
                    log.error("Error reply without destination path: Header: {}, Body: {}", header, body);
                } else {
                    log.error("No response mapper found for path: {}", header.path);
                }
            } else {
                SvBusResponse response = pathMatcher.convertResponse(body, hr);
                rl.setResponse(response);
            }
        }
    }

    void handleRequest(JsonNode body, SvChannelHeader header) {
        HandlerRecord handlerRec = findRqByPath(header.path);
        SvBusResponse response;
        if (handlerRec != null) {
            SvBusRequest rq = pathMatcher.convertRequest(body, handlerRec);
            try {
                response = handlerRec.handler.handleRequest(rq, header);
            } catch (Exception e) {
                log.error("Error in handler method for path {}", handlerRec.pathSpec, e);
                response = errornousRequestsHandler(body, header, SvBusErrorCodes.PROCESSING_ERROR, e.getMessage());
            }
            replyTo(header, response);
        } else {
            response = errornousRequestsHandler(body, header, SvBusErrorCodes.NO_HANDLER, "No handler registered.");
            replyTo(header, response);
        }
    }

    void handleIncoming(SvChannelHeader header, JsonNode body) {

        if (!isMyAddress(header.to)) {
            int res = routeMessage(header, body);
        } else {
            //TODO maybe better to encapsulate such logic inside header?
            if (header.inResponseTo != null) { //this is response
                handleResponse(body, header);
            } else {
                handleRequest(body, header);
            }
        }
    }

    void onConnectionUp(URI uri) {

        SvBusClient client = connections.get(uri);
        //should never happend
        if (client == null) {
            log.error("Connection {} was not added", uri);
            return;
        }
        List<SvChannelMessage> toRemove = new ArrayList<>();
        //first, say Hello
        sendHello(uri);

        boolean def = connections.isDefault(uri);
        for (SvChannelMessage m : outgoingQueue) {
            if (def || m.header.to.equals(uri.toString())) {
                try {
                    if (client.sendMessage(m)) {
                        toRemove.add(m);
                    }
                } catch (JsonProcessingException ex) {
                    log.error("Can not map message to JSON");
                }
            }
        }

        outgoingQueue.removeAll(toRemove);
    }

    void shutdown() {
        executor.shutdown();
        connections.close();
    }

    void setMyInfo(SvBusHello info) {
        hello = info;
        myAddress = URI.create(info.clientAddr);
    }

    private int routeMessage(SvChannelHeader header, JsonNode body) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
