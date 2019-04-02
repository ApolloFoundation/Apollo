/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * Abstraction level for Peer To Peer transport.
 * @author alukin@gmail.com
 */
public class PeerClientTransport implements Closeable {
    private WeakReference<PeerImpl> peerWR;
    private PeerWebSocket ws;
    private HttpURLConnection httpConnection;
    
    public PeerClientTransport(PeerImpl peer) {
        peerWR=new WeakReference<>(peer);
    }
    
    public boolean connect(){
        boolean res = false;
        PeerImpl  peer = peerWR.get();
        if(peer!=null){
            
        }
        return res;
    }
    
    public JSONObject sendAndGetResponse(final JSONStreamAware request, int maxResponseSize){
        return null;
    }

    @Override
    public void close() throws IOException {
        
    }
}
