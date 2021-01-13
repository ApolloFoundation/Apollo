/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.ConcurrentSelfCleaningHashMap;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.google.common.util.concurrent.TimeLimiter;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author alukin@gmail.com
 */
@Slf4j
public class Peer2PeerTransport {
    private static final AtomicLong counter = new AtomicLong(0);

    private final Peer peer;
    /**
     * map requests to responses
     */
    private static final ConcurrentSelfCleaningHashMap<Long, ResponseWaiter> requestMap = new ConcurrentSelfCleaningHashMap<>(60_000, 60_000, ResponseWaiter::isOld);
    private final Random rnd = new Random();
    private final PeerServlet peerServlet;
    private final Object volumeMonitor = new Object();
    @Getter
    private final TimeLimiter limiter;
    private volatile long downloadedVolume;
    private volatile long uploadedVolume;
    private volatile PeerWebSocket inboundWebSocket;
    //this should be final because it is problematic to stop websocket client properly
    private volatile PeerWebSocketClient outboundWebSocket;
    @Getter
    private long lastActivity;
    private final long number;

    public Peer2PeerTransport(@NonNull Peer peer, @NonNull PeerServlet peerServlet, @NonNull TimeLimiter limiter) {
        this.peer = peer;
        this.peerServlet = peerServlet;
        lastActivity = System.currentTimeMillis();
        this.limiter = limiter;
        this.number = counter.incrementAndGet();
    }

    //we use random numbers to minimize possible request/response mismatches
    private Long nextRequestId() {
        return rnd.nextLong();
    }

    public Peer getPeer() {
        return peer;
    }

    String which() {
        String which;
        if (inboundWebSocket != null) {
            which = "Inbound";
        } else if (outboundWebSocket != null && outboundWebSocket.isClientConnected()) {
            which = "Outbound, connected";
        } else {
            which = "Outbound, not connected";
        }
        which += " at " + peer.getHostWithPort();
        return which;
    }

    public long getDownloadedVolume() {
        return downloadedVolume;
    }

    void updateDownloadedVolume(long volume) {
        synchronized (volumeMonitor) {
            downloadedVolume += volume;
        }
        //TODO: do we need this here?
        // PeersService.notifyListeners(getPeer(), PeersService.Event.DOWNLOADED_VOLUME);
    }

    public long getUploadedVolume() {
        return uploadedVolume;
    }

    void updateUploadedVolume(long volume) {
        synchronized (volumeMonitor) {
            uploadedVolume += volume;
        }
        //TODO: do we need this here?
        // PeersService.notifyListeners(getPeer(), PeersService.Event.UPLOADED_VOLUME);
    }

    public void onIncomingMessage(String message, PeerWebSocket ws, Long rqId) {
        if (rqId == null) {
            log.debug("Protocol error, requestId=null from {}, message:\n{}\n", which(), message);
        } else {
            ResponseWaiter wsrw = requestMap.getValue(rqId);
            if (wsrw != null) { //this is response we are waiting for
                wsrw.setResponse(message);
            } else {
                //most likely ge've got request from remote and should process it
                //but it also can be error response without requestId
                log.trace("Receive new request {} - transport {}", rqId, number);
                peerServlet.doPostWebSocket(this, rqId, message);
            }
        }
        lastActivity = System.currentTimeMillis();
        updateDownloadedVolume(message.length());
    }

    public Long sendRequest(String message) {
        Long requestId = sendRequestNoResponseWaiter(message);
        if (requestId != null) {
            requestMap.putValue(requestId, new ResponseWaiter());
        }
        return requestId;
    }

    private Long sendRequestNoResponseWaiter(String message) {
        Long requestId = nextRequestId();
        boolean sendOK = send(message, requestId);
        if (sendOK) {
            return requestId;
        } else {
            return null;
        }
    }

    public String sendAndWaitResponse(String request) {
        String res = null;
        Long rqId;
        boolean sendOK = true;
        rqId = sendRequest(request);
        if (rqId == null) {
            log.debug("Exception while sending to websocket of {}", which());
            sendOK = false;
        }
        if (sendOK) {
            res = getResponse(rqId);
        }
        return res;
    }

    public String getResponse(Long rqId) {
        String res = null;
        ResponseWaiter wsrw = requestMap.getValue(rqId);
        if (wsrw != null) {
            try {
                res = wsrw.get(PeersService.readTimeout);
            } catch (SocketTimeoutException ex) {
                log.trace("Timeout exceeded while waiting response from: {} ID: {}", which(), rqId);
            }
            requestMap.remove(rqId);
        } else {
            log.error("Waiting for non-existent request. Peer: {}, ID: {}", which(), rqId);
        }
        return res;
    }

    public String getHostWithPort() {
        String res = peer.getHostWithPort();
        return res;
    }

    public void onWebSocketClose(PeerWebSocket ws) {
        log.trace("Peer: {} websocket close", which());
        peer.deactivate("Websocket close event");
    }

    private boolean sendToWebSocket(final String wsRequest, PeerWebSocket ws, Long requestId) {
        boolean sendOK = false;
        try {
            if (ws == null) {
                log.debug("null websocket");
                return sendOK;
            }
            log.trace("Send request {}, transport {}", requestId, number);
            sendOK = ws.send(wsRequest, requestId);
        } catch (IOException ex) {
            log.debug("Can't sent to " + getHostWithPort(), ex);
        }
        return sendOK;
    }

    public boolean send(String message, Long requestId) {
        boolean sendOK = false;
        if (StringUtils.isBlank(message )) {
            //we have nothing to send
            return sendOK;
        }
        log.trace("Send request {} - transport {}", requestId, number);
        if (isInbound()) {
            sendOK = sendToWebSocket(message, inboundWebSocket, requestId);
            if (!sendOK) {
                log.trace("Peer: {} Using inbound web socket. failed. Closing", getHostWithPort());
                if (inboundWebSocket != null) {
                    inboundWebSocket.close();
                    inboundWebSocket = null;
                }
            } else {
                log.trace("Peer: {} Send using inbound web socket failed", getHostWithPort());
            }
        }
        if (!sendOK) { //no inbound connection or send failed
            if (outboundWebSocket == null) {
                outboundWebSocket = new PeerWebSocketClient(this);
            }
            if (!outboundWebSocket.isClientConnected()) {
                // Create a new WebSocket session if we don't have one
                // and do not have inbound
                String addrWithPort = peer.getAnnouncedAddress();
                if (!StringUtils.isBlank(addrWithPort)) { // we cannot use peers that do not have external address
                    String wsConnectString = "ws://" + addrWithPort + "/apl";
                    URI wsUri = URI.create(wsConnectString);
                    log.trace("Connecting to websocket'{}'...", wsConnectString);
                    sendOK = outboundWebSocket.startClient(wsUri);
                    if (sendOK) {
                        log.trace("Connected as client to websocket {}", wsConnectString);
                    }
                }
            } else { //client socket is already open
                sendOK = true;
            }
            if (sendOK) { //send using client socket
                sendOK = sendToWebSocket(message, outboundWebSocket, requestId);
            }
        }
        if (!sendOK) {
            String msg = "Error on sending request";
            Peer p = getPeer();
            if (p != null) {
                p.deactivate(msg);
            }
        } else {
            updateUploadedVolume(message.length());
        }
        return sendOK;
    }

    void disconnect() {
        if (inboundWebSocket != null) {
            inboundWebSocket.close();
            inboundWebSocket = null;
        }
        if (outboundWebSocket != null) {
            outboundWebSocket.close();
            outboundWebSocket = null;
        }

    }

    boolean isInbound() {
        boolean res = inboundWebSocket != null && inboundWebSocket.isConnected();
        return res;
    }

    boolean isOutbound() {
        boolean res = outboundWebSocket != null && outboundWebSocket.isConnected();
        return res;
    }

    void setInboundSocket(PeerWebSocket pws) {
        inboundWebSocket = pws;
    }

    @Override
    public String toString() {
        return "Peer2PeerTransport{" +
            "peer=" + peer +
            ", isInbound=" + isInbound() +
            ", isOutbound=" + isOutbound() +
            ", downloadedVolume=" + downloadedVolume +
            ", uploadedVolume=" + uploadedVolume +
            ", lastActivity=" + lastActivity +
            '}';
    }
}
