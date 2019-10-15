/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.client.common.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PeerWebSocketClient extends PeerWebSocket{

    private WebSocketClient client;
    private boolean connected = false;
    
    public PeerWebSocketClient(Peer2PeerTransport peer) {
        super(peer);
        client = new WebSocketClient();
        client.getPolicy().setIdleTimeout(PeersService.webSocketIdleTimeout);
        client.getPolicy().setMaxBinaryMessageSize(PeersService.MAX_MESSAGE_SIZE);
        client.setStopAtShutdown(true);        
    }
    
    public synchronized boolean startClient(URI uri) {
        if (uri == null) {
            return false;
        }
        if(connected){ //we want just one session, not more
            return true;
        }
        try {
            client.start();
            Future<Session> conn = client.connect(this, uri);
            Session session = conn.get(PeersService.connectTimeout + 100, TimeUnit.MILLISECONDS);
            connected = session.isOpen();
        } catch (InterruptedException ex) {
            log.trace("Interruped while connecting as client to: {} \n Exception: {}",which());
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.trace("Execution failed while connecting as client to: {} \n Exception: {}",which());
        } catch (TimeoutException ex) {
            log.trace("Timeout exceeded while connecting as client to: {} \n Exception: {}",which());
        } catch (IOException ex) {
            log.trace("I/O error while connecting as client to: {} \n Exception: {}",which());
        } catch (Exception ex) {
            log.trace("Generic error while connecting as client to: {} \n Exception: {}",which());
        }

        return connected;
    }

    @Override
    public void close() {
        super.close();
        connected = false;
        if (client != null) {
          for(WebSocketSession wss: client.getOpenSessions()){
              wss.disconnect();
              wss.close();
              if(wss!=null){
                wss.destroy();
              }
          }
          destroyClient();
        }
    }

    private void destroyClient() {
        if (client == null) {
            return;
        }
        try {
            if (client.isRunning()) {
                client.stop();
            }

        } catch (Exception ex) {
            log.trace("Exception on websocket client stop", ex);
        }
        if (client != null) {
            client.destroy();
        }
        client = null;
        log.trace("WebSocketClient: {} destroyed.", which());
    }

    boolean isClientConnected() {
        return connected;
    }

}
