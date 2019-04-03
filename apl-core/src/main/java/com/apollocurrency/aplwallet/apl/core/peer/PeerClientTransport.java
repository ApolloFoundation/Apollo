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
    private final WeakReference<PeerImpl> peerWR;
    private PeerWebSocket ws;
    private HttpURLConnection httpConnection;
    private boolean isSecure;
    
    public PeerClientTransport(PeerImpl peer) {
        peerWR=new WeakReference<>(peer);
    }
    
    public boolean connect(){
        boolean res = false;
        PeerImpl  peer = peerWR.get();
        if(peer!=null){
            if(Peers.useWebSockets){
                res = connectWebSockets();
            }
            if(!res){
                res = connectHttpsOrHttp();
            }                
        }
        return res;
    }

    public boolean isIsSecure() {
        return isSecure;
    }
    
    public JSONObject sendAndGetResponse(final JSONStreamAware request, int maxResponseSize){
        return null;
    }

    @Override
    public void close() throws IOException {
        if(ws!=null){
            ws.close();
            ws=null;
        }
        if(httpConnection!=null){
            httpConnection.disconnect();
            httpConnection=null;
        }                
    }
    
    private boolean connectWebSockets(){
        boolean res;
        if(!(res=connectWSS())){
            res = connectWS();
            isSecure = false;
        }
        return res;
    }
    
    private boolean connectHttpsOrHttp(){
        boolean res;
        if(!(res=connectHTTPS())){
            res = connectHTTP();
            isSecure = false;
        }
        return res;        
    }
    /** 
     * try to connect using secure web sockets
     */ 
    private boolean connectWSS() {
       boolean res = false;
       if(res){
           isSecure=true;
       }
       return res;
    }
    /** 
     * try to connect using insecure web sockets
     */ 
    private boolean connectWS() {
       boolean res = false;
       isSecure=false;
       return res;
    }
    
    /** 
     * try to connect using secure https
     */ 
    private boolean connectHTTPS() {
       boolean res = false;
       if(res){
           isSecure=true;
       }
       return res;
    }
    /** 
     * try to connect using issecure http
     */ 
    private boolean connectHTTP() {
       boolean res = false;
       isSecure=false;
       return res;
    }
    
    
}
