/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.google.common.util.concurrent.Monitor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author alukin@gmail.com
 */
@Slf4j
public class PeerWebSocketClient extends PeerWebSocket {

    private static volatile WebSocketClient client = null;
    private final Monitor startMonitor = new Monitor();
    private Session session = null;

    public PeerWebSocketClient(Peer2PeerTransport peer) {
        super(peer);
        if (client == null) {
            synchronized (PeerWebSocketClient.class) {
                if (client == null) {
                    try {
                        init();
                    } catch (Exception ex) {
                        log.error("Can not start wesocket client", ex);
                    }
                }
            }
        }
    }

    private static void init() throws Exception {
        client = new WebSocketClient();
        client.getPolicy().setIdleTimeout(PeersService.webSocketIdleTimeout);
        client.setConnectTimeout(PeersService.connectTimeout);
        client.getPolicy().setMaxBinaryMessageSize(PeersService.MAX_MESSAGE_SIZE);
        client.setStopAtShutdown(true);
        client.start();
    }

    public static void destroyClient() {
        if (client == null) {
            synchronized (PeerWebSocketClient.class) {
                if (client == null) {
                    return;
                }
            }
        }
        try {
            //if (client.isRunning()) {
            //need to stop the client anyway
            client.stop();
            //}

        } catch (Exception ex) {
            log.trace("Exception on websocket client stop", ex);
        }
        if (client != null) {
            client.destroy();
        }
        client = null;
    }

    public boolean startClient(URI uri) {
        boolean connected = false;
        if (uri == null) {
            return false;
        }
        if (isConnected()) { //we want just one session, not more
            return true;
        }
        //synchronizing here
        startMonitor.enter();
        try {

            Future<Session> conn = client.connect(this, uri);
            session = conn.get(PeersService.connectTimeout + 100, TimeUnit.MILLISECONDS);
            connected = session.isOpen();
        } catch (InterruptedException ex) {
            log.trace("Interrupted while connecting as client to: {}", which());
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.trace("Execution failed while connecting as client to: {}", which());
        } catch (TimeoutException ex) {
            log.trace("Timeout exceeded while connecting as client to: {}", which());
        } catch (IOException ex) {
            log.trace("I/O error while connecting as client to: {}", which());
        } catch (Exception ex) {
            log.trace("Generic error while connecting as client to: {}", which());
        } finally {
            startMonitor.leave();
        }

        return connected;
    }

    @Override
    public void close() {
        try {
            super.close();
            if (isClientConnected()) {
                session.disconnect();
                session.close();
                session = null;
            }
        } catch (IOException ex) {
            log.warn("Can not close websocket");
        }
    }

    boolean isClientConnected() {
        return session != null && session.isOpen();
    }

}
