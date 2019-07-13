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
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public PeerWebSocketClient(PeerImpl peer) {
        super(peer); 
        client = new WebSocketClient();
        client.getPolicy().setIdleTimeout(Peers.webSocketIdleTimeout);
        client.getPolicy().setMaxBinaryMessageSize(Peers.MAX_MESSAGE_SIZE);
    }


//    public PeerWebSocketClient(PeerImpl peer) {

//    }

    public boolean startClient(URI uri) {
        if (uri == null) {
            return false;
        }
        boolean websocketOK = false;
        try {
            client.start();
            ClientUpgradeRequest req = new ClientUpgradeRequest();
            Future<Session> conn = client.connect(this, uri, req);
            Session session = conn.get(Peers.connectTimeout + 100, TimeUnit.MILLISECONDS);
            websocketOK = session.isOpen();
        } catch (InterruptedException ex) {
            log.debug("Interruped while connecting as client to: {} \n Exception: {}",getPeer().getHostWithPort());
        } catch (ExecutionException ex) {
            log.debug("Execution failed while connecting as client to: {} \n Exception: {}",getPeer().getHostWithPort());
        } catch (TimeoutException ex) {
            log.debug("Timeout exceeded while connecting as client to: {} \n Exception: {}",getPeer().getHostWithPort());
        } catch (IOException ex) {
            log.debug("I/O error while connecting as client to: {} \n Exception: {}",getPeer().getHostWithPort());
        } catch (Exception ex) {
            log.debug("Generic error while connecting as client to: {} \n Exception: {}",getPeer().getHostWithPort());
        }

        return websocketOK;
    }
}
