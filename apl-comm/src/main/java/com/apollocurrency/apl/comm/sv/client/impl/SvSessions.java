/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.apl.comm.sv.client.impl;

import com.apollocurrency.apl.comm.sv.client.ConnectionStatus;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Set of all connections inited
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class SvSessions {

    private final Map<URI, SvBusClient> connections = new ConcurrentHashMap<>();
    private final Timer timer;
    public static final long CONNECT_INTERVAL_MS = 5000;
    private Map.Entry<URI, SvBusClient> defaultConnection;
    private final MessageDispatcherImpl dispatecher;

    public SvSessions(MessageDispatcherImpl dispatecher) {
        this.dispatecher = dispatecher;
        //init connection restore timer task
        //TODO: use our thread pool manager maybe
        //TODO: ScheduledThreadPoolExecutor is preferred to Timer, better to use it
        timer = new Timer("SypervisorConnectionChekcer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (URI key : connections.keySet()) {
                    SvBusClient client = connections.get(key);
                    if (!client.isConnected() && client.getState() != ConnectionStatus.CONNECTING) {
                        boolean res = client.connect();
                        log.debug("Connection attempt to URI {} result: {}, state: {}", key, res,
                                client.getState()
                        );
                        if (res) {
                            dispatecher.onConnectionUp(key);
                        }
                    }
                }
            }

        }, 0, CONNECT_INTERVAL_MS);
    }

    public Map<URI, SvBusClient> getAll() {
        return connections;
    }

    void put(URI uri, SvBusClient client, boolean isDefault) {
        boolean setDefault = false;
        //first one is default even if isDefault is false
        if (connections.isEmpty()) {
            setDefault = true;
        }
        connections.putIfAbsent(uri, client);
        if (setDefault || isDefault) {
            defaultConnection = connections.entrySet().iterator().next();
        }
    }

    boolean isDefault(URI addr) {
        return defaultConnection != null && defaultConnection.getKey().equals(addr);
    }

    SvBusClient get(URI addr) {
        return connections.get(addr);
    }

    Map.Entry<URI, SvBusClient> getDefault() {
        return defaultConnection;
    }

    void close() {
        timer.cancel();
        connections.values().forEach(c -> {
            c.close();
        });
    }

}
