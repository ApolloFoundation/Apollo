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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

/**
 *
 * @author al
 */
@Slf4j
public class PeerWebSocketClient {

    private WebSocketClient client;
    private final PeerWebSocket endpoint;
    private final PeerImpl peer;

    public PeerWebSocketClient(PeerImpl peer) {
        this.peer = peer;
        endpoint = new PeerWebSocket(peer);
    }

    public boolean startClient(URI uri) {
        if (uri == null) {
            return false;
        }
        boolean websocketOK = false;
        try {
            String address = String.format("%s:%d", uri.getHost(), uri.getPort());
            ClientUpgradeRequest req = new ClientUpgradeRequest();
            Future<Session> conn = client.connect(this, uri, req);
            Session session = conn.get(Peers.connectTimeout + 100, TimeUnit.MILLISECONDS);
            websocketOK = session.isOpen();
        } catch (InterruptedException ex) {
            //Logger
        } catch (ExecutionException ex) {
            //Logger
        } catch (TimeoutException ex) {
            //Logger
        } catch (IOException ex) {
            //Logger
        }

        return websocketOK;
    }

    public String sendAndWaitResponse(String request) throws IOException {
        Long rqId = endpoint.send(request);
        String res = getResponse(rqId);
        return res;
    }

    public Long send(String request) throws IOException {
        return endpoint.send(request);
    }

    public String getResponse(Long requestId) {
        return endpoint.getResponse(requestId);
    }

}
