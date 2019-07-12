/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PeerWebSocket extends  WebSocketAdapter{
    private final PeerImpl peer;
    private long nextRequestId = 0;
    /** Our WebSocket message version */
    private static final int WS_VERSION = 1;    
    /** Negotiated WebSocket message version */
    private int version = WS_VERSION;
      /** Compressed message flag */
    private static final int FLAG_COMPRESSED = 1;
    
    public PeerWebSocket(PeerImpl peer) {
        this.peer = peer;
    }
    
    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message); 
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause); 
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess); 
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        super.onWebSocketBinary(payload, offset, len); 
    }
    
    public void sendAndWaitResponse(String request){
        
    }
    
    public Long send(String request) throws IOException{
        Long requestId=nextRequestId++;
        Session session = getSession();
            if (session == null || !session.isOpen()) {
                String msg = String.format("WebSocket session is not open for peer %s", peer==null?"null":peer.getHostWithPort());
                log.debug(msg);
                throw new IOException(msg);
            }        
            byte[] requestBytes = request.getBytes("UTF-8");
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
    
    public String getResponse(Long requestId){
        return "";
    } 
}
