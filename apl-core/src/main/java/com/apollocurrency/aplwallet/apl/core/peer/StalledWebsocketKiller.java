/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.util.Constants;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;


/**
 * Client web socket creates thread and to many thereads is problem
 * so we should find all stalled client wesockets and kill them all
 * @author alukin@gmail.com
 */
@Slf4j
public class StalledWebsocketKiller {

    private static final List<PeerWebSocketClient> clientWebsockets = new ArrayList<>();
    void register(PeerWebSocketClient wsc){
        clientWebsockets.add(wsc);
    }
    boolean isBad(PeerWebSocketClient wsc){
          String reason="";
          long now = System.currentTimeMillis();

          Peer2PeerTransport tr = wsc.getTransport();
          Peer p;
          
          boolean noTransport=true;
          boolean noPeeer=true;
          boolean notConnectedState = true;
          
          if(tr!=null){
              p=tr.getPeer();
              noTransport=false;
          }else{
              reason+="Lost transport";
              p=null;
          }
          if(p==null){
              reason+=" Lost peer";
          }else{
            noPeeer=false;
            //we could be just in connecting process so...
            notConnectedState = p.getState()!=PeerState.CONNECTED 
                     && now - tr.getLastActivity() >Constants.PEER_UPDATE_INTERVAL*1000/2;
            if(notConnectedState){
                reason+=" Not connectd peer";
            }
          }

          boolean idleLongTime = now -  wsc.getLastActivityTime() >= Peers.webSocketIdleTimeout;
          if(idleLongTime){
              reason+="Idle timeout";
          }
          boolean bad =  noPeeer || noTransport || idleLongTime||notConnectedState;
          
          if(bad){
              String addr="Unknown";
              Session s = wsc.getSession();
              if(s!=null){
                  addr = s.getRemoteAddress().getHostName()+":"+s.getRemoteAddress().getPort();
              }
            log.debug("Bad websocet client found. Addr: {} Reason:{}",addr,reason);
          }
          return bad;
    }
    
    void killStalled(){
 
        int counter=0;
        for(PeerWebSocketClient wsc :clientWebsockets){

          if( isBad(wsc) ){
              Peer2PeerTransport transport = wsc.getTransport();
              if(transport!=null){
                  transport.onWebSocketClose(wsc);
              }else{
                  wsc.close();
              }
              counter++;
          }
        }
      log.debug("Peers: {}, WebSocketClients total: {}, Stalled webSocket clients killed: {}",
              Peers.getAllConnectablePeers().size(),
              clientWebsockets.size(),
              counter);
    }

    void unregister(PeerWebSocketClient wsc) {
        clientWebsockets.remove(wsc);
    }

    int count() {
        return clientWebsockets.size();
    }
}
