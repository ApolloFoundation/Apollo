/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author al
 */
class PeerConnectingThread implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(PeerConnectingThread.class);
    private EpochTime timeService;
    
    public PeerConnectingThread(EpochTime timeService) {
        this.timeService=timeService;
    }

    @Override
    public void run() {
        if (Peers.shutdown || Peers.suspend) {
            return;
        }
        try {
            try {
                final int now = timeService.getEpochTime();
                if (!Peers.hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                    List<Future<?>> futures = new ArrayList<>();
                    List<Peer> hallmarkedPeers = Peers.getPeers((peer) -> !peer.isBlacklisted() && peer.getAnnouncedAddress() != null && peer.getState() != Peer.State.CONNECTED && now - peer.getLastConnectAttempt() > 600 && peer.providesService(Peer.Service.HALLMARK));
                    List<Peer> nonhallmarkedPeers = Peers.getPeers((peer) -> !peer.isBlacklisted() && peer.getAnnouncedAddress() != null && peer.getState() != Peer.State.CONNECTED && now - peer.getLastConnectAttempt() > 600 && !peer.providesService(Peer.Service.HALLMARK));
                    if (!hallmarkedPeers.isEmpty() || !nonhallmarkedPeers.isEmpty()) {
                        Set<PeerImpl> connectSet = new HashSet<>();
                        for (int i = 0; i < 10; i++) {
                            List<Peer> peerList;
                            if (hallmarkedPeers.isEmpty()) {
                                peerList = nonhallmarkedPeers;
                            } else if (nonhallmarkedPeers.isEmpty()) {
                                peerList = hallmarkedPeers;
                            } else {
                                peerList = (ThreadLocalRandom.current().nextInt(2) == 0 ? hallmarkedPeers : nonhallmarkedPeers);
                            }
                            connectSet.add((PeerImpl) peerList.get(ThreadLocalRandom.current().nextInt(peerList.size())));
                        }
                        connectSet.forEach((peer) -> futures.add(Peers.peersExecutorService.submit(() -> {
                            peer.handshake(Peers.blockchainConfig.getChain().getChainId());
                            if (peer.getState() == Peer.State.CONNECTED && Peers.enableHallmarkProtection && peer.getWeight() == 0 && Peers.hasTooManyOutboundConnections()) {
                                LOG.debug("Too many outbound connections, deactivating peer " + peer.getHost());
                                peer.deactivate();
                            }
                            return null;
                        })));
                        for (Future<?> future : futures) {
                            future.get();
                        }
                    }
                }
                Peers.peers.values().forEach((peer) -> {
                    if (peer.getState() == Peer.State.CONNECTED && now - peer.getLastUpdated() > 3600 && now - peer.getLastConnectAttempt() > 600) {
                        Peers.peersExecutorService.submit(() -> peer.handshake(Peers.blockchainConfig.getChain().getChainId()));
                    }
                    if (peer.getLastInboundRequest() != 0 && now - peer.getLastInboundRequest() > Peers.webSocketIdleTimeout / 1000) {
                        peer.setLastInboundRequest(0);
                        Peers.notifyListeners(peer, Peers.Event.REMOVE_INBOUND);
                    }
                });
                if (Peers.hasTooManyKnownPeers() && Peers.hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                    int initialSize = Peers.peers.size();
                    for (PeerImpl peer : Peers.peers.values()) {
                        if (now - peer.getLastUpdated() > 24 * 3600) {
                            peer.remove();
                        }
                        if (Peers.hasTooFewKnownPeers()) {
                            break;
                        }
                    }
                    if (Peers.hasTooManyKnownPeers()) {
                        PriorityQueue<PeerImpl> sortedPeers = new PriorityQueue<>(Peers.peers.values());
                        int skipped = 0;
                        while (skipped < Peers.minNumberOfKnownPeers) {
                            if (sortedPeers.poll() == null) {
                                break;
                            }
                            skipped += 1;
                        }
                        while (!sortedPeers.isEmpty()) {
                            sortedPeers.poll().remove();
                        }
                    }
                    LOG.debug("Reduced peer pool size from " + initialSize + " to " + Peers.peers.size());
                }
                for (String wellKnownPeer : Peers.wellKnownPeers) {
                    PeerImpl peer = Peers.findOrCreatePeer(wellKnownPeer, true);
                    if (peer != null && now - peer.getLastUpdated() > 3600 && now - peer.getLastConnectAttempt() > 600) {
                        Peers.peersExecutorService.submit(() -> {
                            Peers.addPeer(peer);
                            Peers.connectPeer(peer);
                        });
                    }
                }
            } catch (Exception e) {
                LOG.debug("Error connecting to peer", e);
            }
        } catch (Throwable t) {
            LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
            //TODO: do we really shoud exit here? Can we recover?
            System.exit(1);
        }
    }
    
}
