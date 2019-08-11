/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PeerWebSocket extends WebSocketAdapter {

    /** we use reference here to avoid memory leaks */
    private final SoftReference<Peer2PeerTransport> peerTransportSoftReference;

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
    private long lastActivityTime;

    public PeerWebSocket(Peer2PeerTransport peerTransport) {
        log.trace("Create WebSocket to peer+Transport = {}", peerTransport.getPeer());
        peerTransportSoftReference = new SoftReference<>(peerTransport);
        lastActivityTime=System.currentTimeMillis();
    }
    
    String which(){
        String which="";
        Peer2PeerTransport p = peerTransportSoftReference.get();
        if (p != null) {
            which+=" at " + p.getHostWithPort();
        } else {
            Session s = super.getSession();
            if (s != null) {
                RemoteEndpoint r = s.getRemote();
                if(r != null) {
                    String addr = r.getInetSocketAddress().getAddress().getHostAddress();
                    which = addr + ":" + r.getInetSocketAddress().getPort();
                } else {
                    which += "'unknown' remoteAddr?";
                }
            } else {
                which += "'unknown' WS session?";
            }
        }
        return which;
    }
    
    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        lastActivityTime=System.currentTimeMillis();
        log.debug("Peer String received for ''{}'\n{}", which(), message);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        log.trace("Peer on WebSocket error: '{}', '{}'", which(), cause);
        if(peerTransportSoftReference.get()==null){
            this.close();
        }
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        log.trace("onWebSocketConnect: '{}'", which());
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        log.trace("Peer WebSocket close for '{}' : code={}", which(), statusCode);
        Peer2PeerTransport p = peerTransportSoftReference.get();
        if(p != null){
            p.onWebSocketClose(this);
        }else{
            log.debug("Closing orphaned websocket: '{}'",which());
            this.close();
            peerTransportSoftReference.clear();
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        super.onWebSocketBinary(payload, offset, len);
        log.trace("Process incoming data... for {}", this.peerTransportSoftReference.get());
        lastActivityTime=System.currentTimeMillis();
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
            Peer2PeerTransport p = peerTransportSoftReference.get();
            if(p!=null){
                p.onIncomingMessage(message,this,rqId);
            }else{
                log.warn("Peer reference is null on websocket incoming message, closing websocket:\n {}",message);
                this.close();
            }    

        } catch (IOException ex) {
            log.debug("Peer: {} IO Exception on message receiving: " + which(), ex);
        }
    }

    /**
     * Sends websocket message
     * Must be synchronized because it is used from multiple threads
     * @param message message string
     * @param requestId if it is not null, it means it is request otherwise it is
     * response
     * @return requestId
     * @throws IOException
     */
    public synchronized boolean send(String message, Long requestId) throws IOException {
        if(StringUtils.isBlank(message.trim())){
            log.warn("Empty request from us to {}",which());
            return false;
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
        Session s =  super.getSession();
        if(s!=null){
           s.getRemote().sendBytes(buf);
        }else{
          throw new IOException("Websocket session is null for " + which())  ;
        }
        return true;
    }

    public void close(){
        Session s = super.getSession();
        if (s!=null) {
            s.close(1001,"Disconnect"); //RFC 6455, Section 7.4.1
            try {
                s.disconnect();                
            } catch (IOException ex) {
                log.debug("Exception on session disconnect to {}",which(),ex);
            }
        }
        peerTransportSoftReference.clear();
    }

    long getLastActivityTime() {
        return lastActivityTime;
    }

    public Peer2PeerTransport getTransport(){
        return peerTransportSoftReference.get();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PeerWebSocket{");
        sb.append("peerTransportSoftReference=").append(
                peerTransportSoftReference != null
                        && peerTransportSoftReference.get() != null ? "assignedPeer" : "null");
        sb.append(", version=").append(version);
        sb.append(", lastActivityTime=").append(lastActivityTime);
        sb.append('}');
        return sb.toString();
    }
}
