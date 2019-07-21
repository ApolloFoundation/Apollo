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

    private final WebSocketClient client;
    private AtomicBoolean started = new AtomicBoolean(false);
    
    public PeerWebSocketClient(Peer2PeerTransport peer) {
        super(peer); 
        client = new WebSocketClient();
        client.getPolicy().setIdleTimeout(Peers.webSocketIdleTimeout);
        client.getPolicy().setMaxBinaryMessageSize(Peers.MAX_MESSAGE_SIZE);
    }

    public synchronized boolean startClient(URI uri) {
        Peers.registerWebSocketClient(this);
        if (uri == null) {
            return false;
        }
        boolean websocketOK = false;
        try {
            client.start();
            started.set(true);
            client.setStopAtShutdown(true);
            ClientUpgradeRequest req = new ClientUpgradeRequest();
            Future<Session> conn = client.connect(this, uri, req);
            Session session = conn.get(Peers.connectTimeout + 100, TimeUnit.MILLISECONDS);
            websocketOK = session.isOpen();
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

        return websocketOK;
    }

    @Override
    public void close() {
        super.close(); 
        destroy();
    }

    synchronized void destroy() {
        Peers.unregisterWebSocketClient(this);
        try {
            client.stop();
        } catch (Exception ex) {
            log.trace("Exception on websocket client stop");
        }
        client.getOpenSessions().stream().map((wss) -> {
            wss.close();
            return wss;
        }).forEach((wss) -> {
            wss.destroy();
        });
        client.destroy();
        log.debug("WebSocketClient: {} destroyed.",which());
    }

    AtomicBoolean isStarted() {
        return started;
    }

}
