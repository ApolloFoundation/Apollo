/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

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
    }
    
    private void newClient() throws Exception{
        client = new WebSocketClient();
        client.getPolicy().setIdleTimeout(Peers.webSocketIdleTimeout);
        client.getPolicy().setMaxBinaryMessageSize(Peers.MAX_MESSAGE_SIZE);
        client.setStopAtShutdown(true);
        client.start();
    }
    
    public synchronized boolean startClient(URI uri) {
        if (uri == null) {
            return false;
        }
        try {
            if(client==null){
               newClient();
            }
            ClientUpgradeRequest req = new ClientUpgradeRequest();
            Future<Session> conn = client.connect(this, uri, req);
            Session session = conn.get(Peers.connectTimeout + 100, TimeUnit.MILLISECONDS);
            connected = session.isOpen();
        } catch (InterruptedException ex) {
            log.trace("Interruped while connecting as client to: {} \n Exception: {}",which());
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
            client.getOpenSessions().stream().map((wss) -> {
                wss.close();
                return wss;
            }).forEach((wss) -> {
                wss.destroy();
            });
        }
    }
    
@PreDestroy
    synchronized void destroyClient() {
        if(client==null){
            return;
        }
        try {
            client.stop();
        } catch (Exception ex) {
            log.trace("Exception on websocket client stop");
        }
        client.destroy();
        client=null;
        log.debug("WebSocketClient: {} destroyed.",which());        
    }

    boolean isClientConnected() {
        return connected;
    }

}
