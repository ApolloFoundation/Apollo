/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.transport.TransportEventDescriptor;
import com.apollocurrency.aplwallet.api.transport.TransportStartRequest;
import com.apollocurrency.aplwallet.api.transport.TransportStatusReply;
import com.apollocurrency.aplwallet.api.transport.TransportStopRequest;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@WebSocket(maxTextMessageSize = 64 * 1024, idleTimeout = Integer.MAX_VALUE)
public class TransportInteractionWebSocket {
    public static final int CONNECTION_WAIT_MS = 300;
    private static Random rand = new Random();
    private static ObjectMapper mapper = new ObjectMapper();
    @Getter
    String remoteIp;
    @Getter
    int remotePort;
    @Getter
    String tunAddr;
    @Getter
    String tunNetMask;
    private String serversJSON = "./servers.json";
    private boolean shuffle = true, uniqueEnable = false;
    private int uniquePort = -1;
    private String logPath = "/var/log";
    @SuppressWarnings("unused")
    private Session session;
    private SecureTransportStatus secureTransportStatus;
    private WebSocketClient client;
    private CountDownLatch closeLatch;

    /**
     * entry point for our interaction routine
     *
     * @param endpointURI - link to connect to transport daemon
     */
    public TransportInteractionWebSocket(URI endpointURI, PropertiesHolder propertiesHolder) {
        try {

            secureTransportStatus = SecureTransportStatus.INITIAL;
            cleanupComParams();

            this.closeLatch = new CountDownLatch(1);
            this.client = getWebSocketClient(propertiesHolder);

            client.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(this, endpointURI, request);
            log.debug("Connecting to : {} ", endpointURI);
            awaitClose(CONNECTION_WAIT_MS, TimeUnit.MILLISECONDS);

        } catch (Exception ex) {
            log.error("WS connection exception: ", ex);
        }

    }

    private WebSocketClient getWebSocketClient(PropertiesHolder propertiesHolder) {

        HttpClient httpClient = new HttpClient();

        if (propertiesHolder.getBooleanProperty("apl.limitHardwareResources", false)) {
            final QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setMaxThreads(propertiesHolder.getIntProperty("apl.wsMaxThreadPoolSize"));
            threadPool.setMinThreads(propertiesHolder.getIntProperty("apl.wsMinThreadPoolSize"));
            threadPool.setName(httpClient.getName());
            httpClient.setExecutor(threadPool);
        }
        WebSocketClient socketClient = new WebSocketClient(httpClient);
        socketClient.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));

        return socketClient;
    }

    /**
     * Template for JSON processing
     *
     * @param serviceClass - specification of the class to be handled
     * @param inputData    - JSON as string
     * @param screening    - whether JSON is screened or not
     * @return T - instance of service class, provided as the 1-st argument of input
     */
    private static <T> T processData(Class<T> serviceClass, String inputData, boolean screening) {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        if (screening) objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        T res = null;
        try {
            res = objectMapper.readValue(inputData, serviceClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * asynchronous routine to cope with websockets
     *
     * @param duration - time of expectation in units
     * @param unit     - TimeUnit description for async events
     */
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnWebSocketConnect
    public void onOpen(Session userSession) throws IOException {
        log.debug("TransportInteractionWebSocket: onOpen");
        this.session = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param statusCode code of error in case of problems
     * @param reason     the reason for connection close
     */
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log.debug("TransportInteractionWebSocket: onClose, code: {}, reason: {} ", statusCode, reason);
        if (this.session != null) {
            this.session.close();
        }
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnWebSocketMessage
    public void onMessage(String message) {
        log.debug("onMessage: {}", message);

        try {
            JsonNode parent = new ObjectMapper().readTree(message);
            if (parent.has("type")) {

                String type = parent.get("type").asText();
                log.debug("type: {}", type);

                if (type.equals("GETSTATUSREPLY")) {
                    log.debug("GetStatusReply");
                    TransportStatusReply transportStatusReply = processData(TransportStatusReply.class, message, false);

                    if (transportStatusReply.status.equals("NOT_LAUNCHED")) {
                        log.debug("received status: not launched, configuring internals");
                        secureTransportStatus = SecureTransportStatus.NOT_LAUNCHED;
                    } else if (transportStatusReply.status.equals("CONNECTED")) {
                        log.debug("received status: CONNECTED, saving parameters");
                        // {"type":"GETSTATUSREPLY","status":"CONNECTED","remoteip":"51.15.249.23","remoteport":"25000","tunaddr":"10.75.110.216","tunnetmask":"255.255.255.0","id":"230"}
                        this.remoteIp = transportStatusReply.remoteip;
                        this.remotePort = transportStatusReply.remoteport;
                        this.tunAddr = transportStatusReply.tunaddr;
                        this.tunNetMask = transportStatusReply.tunnetmask;
                        log.debug("connected to: {} : {} via : {} / {}", remoteIp, remotePort, tunAddr, tunNetMask);
                        secureTransportStatus = SecureTransportStatus.CONNECTED;
                    } else if (transportStatusReply.status.equals("INITIAL")) {
                        // seems that we are dealing with service that have never been started.. Starting it up
                        secureTransportStatus = SecureTransportStatus.NOT_LAUNCHED;
                    }

                } else if (type.equals("STARTREPLY")) {
                    log.debug("StartReply, setting PENDING and waiting for connect event");
                    secureTransportStatus = SecureTransportStatus.PENDING;
                }

            } else if (parent.has("event")) {

                String eventSpec = parent.get("event").asText();
                if (eventSpec.equals("CONNECT")) {
                    // handling json here
                    TransportEventDescriptor transportEventDescriptor = processData(TransportEventDescriptor.class, message, false);
                    this.remoteIp = transportEventDescriptor.remoteip;
                    this.remotePort = transportEventDescriptor.remoteport;
                    this.tunAddr = transportEventDescriptor.tunaddr;
                    this.tunNetMask = transportEventDescriptor.tunnetmask;
                    log.debug("connected to: {} : {} via : {} / {}", remoteIp, remotePort, tunAddr, tunNetMask);
                    secureTransportStatus = SecureTransportStatus.CONNECTED;
                } else if (eventSpec.equals("DISCONNECT")) {
                    TransportEventDescriptor transportEventDescriptor = processData(TransportEventDescriptor.class, message, false);
                    cleanupComParams();
                    log.debug("disconnected from: {} : {} via : {} / {}", remoteIp, remotePort, tunAddr, tunNetMask);
                    secureTransportStatus = SecureTransportStatus.DISCONNECTED;
                }
            }
        } catch (IOException ex) {
            log.error("incoming message processing exception: {}", ex.toString());
        }


    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        session.getRemote().sendString(message, new WriteCallback() {
            @Override
            public void writeFailed(Throwable x) {
                log.error("Sending string '{}' has failed = {}", message, x.getMessage());
                WriteCallback.super.writeFailed(x);
            }

            @Override
            public void writeSuccess() {
                WriteCallback.super.writeSuccess();
            }
        });
    }

    /**
     * Checking out whether the connection is open
     */

    public boolean isOpen() {
        return this.session != null && this.session.isOpen();
    }

    /**
     * Resetting communication parameters
     */

    private void cleanupComParams() {
        this.remoteIp = "";
        this.remotePort = -1;
        this.tunAddr = "";
        this.tunNetMask = "";
    }


    /**
     * Inquiring status of connection from transport service
     */

    private void getSecureTransportStatus() {
        log.trace("TransportInteractionWebSocket: getSecureTransportStatus");
        TransportStopRequest stopRequest = new TransportStopRequest();
        stopRequest.type = "GETSTATUSREQUEST";
        stopRequest.id = rand.nextInt(255);

        try {
            String stopRequestString = mapper.writeValueAsString(stopRequest);
            log.trace("getting status: {}", stopRequestString);
            sendMessage(stopRequestString);
        } catch (JsonProcessingException ex) {
            log.error("TransportInteractionWebSocket: Error while creating Getting Status request: {}", ex.getMessage().toString());
        }

    }


    /**
     * Starting transport
     */

    public void startSecureTransport() {
        // creating start package
        TransportStartRequest startRequest = new TransportStartRequest();
        startRequest.type = "STARTREQUEST";
        startRequest.serversjson = this.serversJSON;
        startRequest.uniqueenable = true;
        startRequest.uniqueport = this.uniquePort;
        startRequest.shuffle = this.shuffle;
        startRequest.logpath = this.logPath;
        startRequest.id = rand.nextInt(255);

        try {
            String startRequestString = mapper.writeValueAsString(startRequest);
            sendMessage(startRequestString);
        } catch (JsonProcessingException ex) {
            log.error("TransportInteractionWebSocket: JSON Error while creating STARTREQUEST : {}", ex.getMessage().toString());
        }

    }

    /**
     * Stopping transport
     */
    public void stopSecureTransport() {
        TransportStopRequest stopRequest = new TransportStopRequest();
        stopRequest.type = "STOPREQUEST";
        stopRequest.id = rand.nextInt(255);

        try {
            String stopRequestString = mapper.writeValueAsString(stopRequest);
            sendMessage(stopRequestString);
        } catch (JsonProcessingException ex) {
            log.error("TransportInteractionWebSocket: JSON Error while creating STOPREQUEST: {}", ex.getMessage().toString());
        }
        if (this.client != null) {
            try {
                this.client.stop();
            } catch (Exception e) {
                log.error("Error stopping Transport WebSocket client", e);
            }
        }
        cleanupComParams();
        secureTransportStatus = SecureTransportStatus.DISCONNECTED;
    }


    /**
     * State machine - tick engine
     */
    void tick() {

        if (isOpen()) {

            switch (secureTransportStatus) {
                case INITIAL: {
                    log.trace("Initial state, need to figure out the status of connection");
                    getSecureTransportStatus();
                    break;
                }

                case NOT_LAUNCHED: {
                    log.debug("not launched, startup required");
                    // launching here
                    startSecureTransport();
                    break;
                }
            }
        }
    }


    /**
     * Starting transport
     *
     * @return status of connection as string
     */
    String getRemoteConnectionStatus() {
        return secureTransportStatus.toString();
    }


    // status of the transport connection
    public enum SecureTransportStatus implements Serializable {
        INITIAL, NOT_LAUNCHED, PENDING, CONNECTED, DISCONNECTED;

        public String toString() {
            switch (this) {
                case INITIAL:
                    return "INITIAL";
                case NOT_LAUNCHED:
                    return "NOT_LAUNCHED";
                case PENDING:
                    return "PENDING";
                case CONNECTED:
                    return "CONNECTED";
                case DISCONNECTED:
                    return "DISCONNECTED";
            }
            ;
            return null;
        }
    }
}
