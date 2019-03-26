/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author al
 */
class UnresolvedPeersAnalyzer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(UnresolvedPeersAnalyzer.class);
    private final List<Future<String>> unresolvedPeers;

    public UnresolvedPeersAnalyzer(List<Future<String>> unresolvedPeers) {
        this.unresolvedPeers = unresolvedPeers;
    }

    @Override
    public void run() {
        for (Future<String> unresolvedPeer : unresolvedPeers) {
            try {
                String badAddress = unresolvedPeer.get(5, TimeUnit.SECONDS);
                if (badAddress != null) {
                    LOG.debug("Failed to resolve peer address: " + badAddress);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOG.debug("Failed to add peer", e);
            } catch (TimeoutException ignore) {
            }
        }
        LOG.debug("Known peers: " + Peers.peers.size());
    }
    
}
