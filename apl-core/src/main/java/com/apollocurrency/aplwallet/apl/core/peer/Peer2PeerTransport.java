/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author alukin@gmail.com
 */
@Slf4j
public class Peer2PeerTransport {

    private final SoftReference<Peer> peerReference;
    /**
     * map requests to responses
     */
    private final Cache<Long, ResponseWaiter> requestCache;
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final SoftReference<PeerServlet> peerServlet;
    private final boolean useWebSocket = PeersService.useWebSockets && !PeersService.useProxy;
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

    public Peer2PeerTransport(Peer peer, PeerServlet peerServlet, TimeLimiter limiter) {
        this.peerReference = new SoftReference<>(peer);
        this.peerServlet = new SoftReference<>(peerServlet);
        lastActivity = System.currentTimeMillis();
        this.limiter = limiter;
        this.requestCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MILLISECONDS)
            .expireAfterWrite(60, TimeUnit.MILLISECONDS)
            .concurrencyLevel(100)
            .build();
    }

    //we use random numbers to minimize possible request/response mismatches
    private Long nextRequestId() {
        return requestCounter.incrementAndGet();
    }

    public Peer getPeer() {
        return peerReference.get();
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
        Peer p = peerReference.get();
        if (p != null) {
            which += " at " + p.getHostWithPort();
        }
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
            ResponseWaiter wsrw = requestCache.getIfPresent(rqId);
            if (wsrw != null) { //this is response we are waiting for
                wsrw.setResponse(message);
            } else {
                //most likely ge've got request from remote and should process it
                //but it also can be error response without requestId
                if (peerServlet.get() != null) {
                    peerServlet.get().doPostWebSocket(this, rqId, message);
                } else {
                    log.info("No soft-ref to peerServlet.get()"); // in general we should never see that log
                }
            }
        }
        lastActivity = System.currentTimeMillis();
        updateDownloadedVolume(message.length());
    }

    public Long sendRequest(String message) {
        Long requestId = sendRequestNoResponseWaiter(message);
        if (requestId != null) {
            requestCache.put(requestId, new ResponseWaiter());
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
        ResponseWaiter wsrw = requestCache.getIfPresent(rqId);
        if (wsrw != null) {
            try {
                res = wsrw.get(PeersService.readTimeout);
            } catch (SocketTimeoutException ex) {
                log.trace("Timeout exceeded while waiting response from: {} ID: {}", which(), rqId);
            }
            requestCache.invalidate(rqId);
        } else {
            log.error("Waiting for non-existent request. Peer: {}, ID: {}", which(), rqId);
        }
        return res;
    }

    public String getHostWithPort() {
        String res = "";
        Peer p = peerReference.get();
        if (p != null) {
            res = p.getHostWithPort();
        }
        return res;
    }

    public void onWebSocketClose(PeerWebSocket ws) {
        log.trace("Peer: {} websocket close", which());
        Peer p = peerReference.get();
        if (p != null) {
            p.deactivate("Websocket close event");
        } else {
            ws.close();
        }
    }
//
//    private void cleanUp() {
//        List<Long> toDelete = new ArrayList<>();
//        if (requestCache.size() != 0) {
//            requestCache.asMap().keySet().stream()
//                .filter(wsw -> (requestCache.getIfPresent(wsw).isOld()))
//                .forEachOrdered(toDelete::add);
//        }
//        requestCache.invalidateAll(toDelete);
//    }

    private boolean sendToWebSocket(final String wsRequest, PeerWebSocket ws, Long requestId) {
        boolean sendOK = false;
        try {
            if (ws == null) {
                log.debug("null websocket");
                return sendOK;
            }
            sendOK = ws.send(wsRequest, requestId);
        } catch (IOException ex) {
            log.debug("Can't sent to " + getHostWithPort(), ex);
        }
        return sendOK;
    }

    public boolean send(String message, Long requestId) {
        boolean sendOK = false;
//        cleanUp();
        if (message == null || message.isEmpty()) {
            //we have nothing to send
            return sendOK;
        }
        if (useWebSocket) {
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
                    Peer p = peerReference.get();
                    if (p == null) {
                        log.debug("Premature destruction of peer");
                        disconnect();
                        return sendOK;
                    }
                    String addrWithPort = p.getAnnouncedAddress();
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
        }
//        else {
////            // Send the request using HTTP if websockets are disabled
////            sendOK = sendHttp(message, requestId);
////            log.debug("Trying to use HTTP requests to {} because websockets failed", getHostWithPort());
////            if (!sendOK) {
////                log.debug("Peer: {} Using HTTP. Failed.", getHostWithPort());
////            }
//        }
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
            "peer=" + peerReference.get() +
            ", useWebSocket=" + useWebSocket +
            ", isInbound=" + isInbound() +
            ", isOutbound=" + isOutbound() +
            ", downloadedVolume=" + downloadedVolume +
            ", uploadedVolume=" + uploadedVolume +
            ", lastActivity=" + lastActivity +
            '}';
    }
}
