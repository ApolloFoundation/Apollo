/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.util.ArrayList;
import java.util.List;


/**
 * Client web socket creates thread and to many thereads is problem
 * so we should find all stalled client wesockets and kill them all
 * @author alukin@gmail.com
 */
public class StalledWebsocketKiller {

    private static final List<PeerWebSocketClient> clientWebsockets = new ArrayList<>();
    void register(PeerWebSocketClient wsc){
        clientWebsockets.add(wsc);
    }
    
    void killStalled(){
        long now = System.currentTimeMillis();
        for(PeerWebSocketClient wsc :clientWebsockets){
          if( now -  wsc.getLastActivityTime() >= Peers.webSocketIdleTimeout){
              Peer2PeerTransport transport = wsc.getTransport();
              if(transport!=null){
                  transport.onWebSocketClose(wsc);
              }else{
                  wsc.close();
              }
          }
        }
    }
}
