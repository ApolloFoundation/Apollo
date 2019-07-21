/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;


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
    
    void killStalled(){
        long now = System.currentTimeMillis();
        int counter=0;
        for(PeerWebSocketClient wsc :clientWebsockets){
          if( now -  wsc.getLastActivityTime() >= Peers.webSocketIdleTimeout){
              Peer2PeerTransport transport = wsc.getTransport();
              if(transport!=null){
                  transport.onWebSocketClose(wsc);
              }else{
                  wsc.close();
              }
              counter++;
          }
        }
      log.debug("{} Stalled webSocket clients killed",counter);
    }
}
