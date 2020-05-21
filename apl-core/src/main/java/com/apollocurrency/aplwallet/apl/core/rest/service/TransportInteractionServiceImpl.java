/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Singleton
public class TransportInteractionServiceImpl implements TransportInteractionService {
    private String wsUrl;

    private TransportInteractionWebSocket transportInteractionWebSocket;

    @Setter
    private volatile boolean done;


    @Inject
    TransportInteractionServiceImpl(PropertiesHolder prop) {
        log.debug("Initializing TransportInteractionServiceImpl");
        wsUrl = prop.getStringProperty("apl.securetransporturl", "ws://localhost:8888/");
        done = false;
    }


    @Override
    public TransportStatusResponse getTransportStatusResponse() {
        TransportStatusResponse transportStatusResponse = new TransportStatusResponse();
        log.debug("getTransportStatusResponse");
        boolean isOpen = transportInteractionWebSocket.isOpen();
        log.debug("isOpen: {}", isOpen);
        transportStatusResponse.controlconnection = transportInteractionWebSocket.isOpen();
        transportStatusResponse.remoteConnectionStatus = transportInteractionWebSocket.getRemoteConnectionStatus();
        transportStatusResponse.remoteip = transportInteractionWebSocket.remoteIp;
        transportStatusResponse.remoteport = transportInteractionWebSocket.remotePort;
        transportStatusResponse.tunaddr = transportInteractionWebSocket.tunAddr;
        transportStatusResponse.tunnetmask = transportInteractionWebSocket.tunNetMask;
        return transportStatusResponse;
    }


    @Override
    public void start() {
        log.debug("Ingition point: ");

        try {
            // open websocket
            transportInteractionWebSocket = new TransportInteractionWebSocket(new URI(wsUrl));
            Runnable task = () -> {
                for (; ; ) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        log.error("Runnable exception: {} ", ex.toString());
                    }
                    transportInteractionWebSocket.tick();
                    if (done) break;
                }

            };
            Thread thread = new Thread(task);
            thread.start();

        } catch (URISyntaxException ex) {
            log.error("URISyntaxException exception: {}", ex.getMessage());
        }

    }

    @Override
    public void startSecureTransport() {
        transportInteractionWebSocket.startSecureTransport();
    }

    @Override
    public void stopSecureTransport() {
        transportInteractionWebSocket.stopSecureTransport();
    }

    @Override
    public void stop() {
        this.done = true;
    }


}