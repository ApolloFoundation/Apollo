/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PeerWebSocket extends WebSocketAdapter {
    
    /** we use reference here to avoid memory leaks */
    private final SoftReference<Peer> peerReference;
    private Random rnd;
    /**
     * Our WebSocket message version
     */
    private static final int WS_VERSION = 1;
    /**
     * Negotiated WebSocket message version
     */
    private int version = WS_VERSION;
    /**
     * Compressed message flag
     */
    private static final int FLAG_COMPRESSED = 1;
    /**
     * map requests to responses
     */
    private final ConcurrentHashMap<Long, WebSocketResonseWaiter> requestMap = new ConcurrentHashMap<>();
    private PeerServlet peerServlet;

    public PeerWebSocket(Peer peer) {
        this(peer, null);
    }

    public PeerWebSocket(Peer peer, PeerServlet peerServlet) {
        peerReference = new SoftReference<>(peer);
        if (peerServlet != null) {
            this.peerServlet = peerServlet;
            ((PeerImpl)peer).setInboundWebSocket(this);
        }
        rnd = new Random(System.currentTimeMillis());
    }
    public Peer getPeer(){
        return peerReference.get();
    }
    //we use random numbers to minimize possible request/response mismatches
    private Long nextRequestId() {
        return rnd.nextLong();
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        log.debug("String received: {}",message);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        log.debug("Websocket error: {}",cause);
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        String which;
        if(peerServlet!=null){
            which = "Inbound";
        }else{
            which = "Outbound";
        }
        
        log.debug("WebSocket connectded: {}:{}",
                sess.getRemoteAddress().getHostString(),
                sess.getRemoteAddress().getPort());
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        super.onWebSocketBinary(payload, offset, len);
        try {
            ByteBuffer buf = ByteBuffer.wrap(payload, offset, len);
            version = Math.min(buf.getInt(), WS_VERSION);
            Long rqId = buf.getLong();
            int flags = buf.getInt();
            int length = buf.getInt();
            byte[] msgBytes = new byte[buf.remaining()];
            buf.get(msgBytes);
            if ((flags & FLAG_COMPRESSED) != 0) {
                ByteArrayInputStream inStream = new ByteArrayInputStream(msgBytes);
                try (GZIPInputStream gzipStream = new GZIPInputStream(inStream, 1024)) {
                    msgBytes = new byte[length];
                    int off = 0;
                    while (off < msgBytes.length) {
                        int count = gzipStream.read(msgBytes, off, msgBytes.length - off);
                        if (count < 0) {
                            throw new EOFException("End-of-data reading compressed data");
                        }
                        off += count;
                    }
                }
            }
            String message = new String(msgBytes, "UTF-8");
            WebSocketResonseWaiter wsrw = requestMap.get(rqId);
            if (wsrw != null) { //this is response
                wsrw.setResponse(message);
            } else { //most likely ge've got request from remote and should process it
                if (peerServlet != null) {
                    peerServlet.doPost(this, rqId, message);
                }
            }
        } catch (IOException ex) {
            log.debug("IO Exception on message receiving: {}", ex);
        }
    }

    public String sendAndWaitResponse(String request) {
        Long rqId = 0L;
        boolean sendOK = true;
        String res = null;
        try {
            rqId = send(request, null);
        } catch (IOException ex) {
            Peer p = peerReference.get();
            String addr = "UNKNOWN";
            if (p != null) {
                addr = p.getHostWithPort();
            }
            log.debug("Interrupted whule sending to websocket of {}", addr);
            sendOK = false;
        }
        if (sendOK) {
            try {
                res = getResponse(rqId);
            } catch (InterruptedException|IOException ex) {
                Peer p = peerReference.get();
                String addr = "UNKNOWN";
                if(p!=null){
                    addr=p.getHostWithPort();
                }
                log.debug("Interrupted whule sending to websocket of {}",addr);
            }
        }
        return res;
    }

    /**
     * Sends websocket message
     * Must be synchronized because it is used from multiple threads
     * @param message message string
     * @param rqId if it is not null, it means it is request otherwise it is
     * response
     * @return requestId
     * @throws IOException
     */
    public  synchronized Long send(String message, Long rqId) throws IOException {
        cleanUp();
        Long requestId;
        if (rqId == null) {
            requestId = nextRequestId();
            requestMap.put(requestId, new WebSocketResonseWaiter());
        } else {
            requestId = rqId;
        }
        Session session = getSession();
        if (session == null || !session.isOpen()) {
            Peer peer = peerReference.get();
            String msg = String.format("WebSocket session is not open for peer %s", peer == null ? "null" : peer.getHostWithPort());
            log.debug(msg);
            throw new IOException(msg);
        }
        byte[] requestBytes = message.getBytes("UTF-8");
        int requestLength = requestBytes.length;
        int flags = 0;
        if (Peers.isGzipEnabled && requestLength >= Peers.MIN_COMPRESS_SIZE) {
            flags |= FLAG_COMPRESSED;
            ByteArrayOutputStream outStream = new ByteArrayOutputStream(requestLength);
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(outStream)) {
                gzipStream.write(requestBytes);
            }
            requestBytes = outStream.toByteArray();
        }
        ByteBuffer buf = ByteBuffer.allocate(requestBytes.length + 20);
        buf.putInt(version)
                .putLong(requestId)
                .putInt(flags)
                .putInt(requestLength)
                .put(requestBytes)
                .flip();
        if (buf.limit() > Peers.MAX_MESSAGE_SIZE) {
            throw new ProtocolException("POST request length exceeds max message size");
        }
        session.getRemote().sendBytes(buf);
        return requestId;
    }

    public String getResponse(Long rqId) throws InterruptedException, IOException {
        String res = null;
        WebSocketResonseWaiter wsrw = requestMap.get(rqId);
        if (wsrw != null) {
            res = wsrw.get(Peers.readTimeout, TimeUnit.MILLISECONDS);
        }
        return res;
    }

    private synchronized void cleanUp() {
        List<Long> toDelete = new ArrayList<>();
        for (Long wsw : requestMap.keySet()) {
            if (requestMap.get(wsw).isOld()) {
                toDelete.add(wsw);
            }
        }
        for (Long key : toDelete) {
            requestMap.remove(key);
        }
    }

    public boolean isOpen() {
        Session session = getSession();
        return (session != null && session.isOpen());
    }

    public void close() {
        Session session = getSession();
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}
