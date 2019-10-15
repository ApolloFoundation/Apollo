/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author al
 */
class PeerConnectingThread implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(PeerConnectingThread.class);
    private final TimeService timeService;
    private final PeersService peers;

    public PeerConnectingThread(TimeService timeService, PeersService peers) {
        this.timeService=timeService;
        this.peers = peers;
    }

    @Override
    public void run() {
        if (peers.shutdown || peers.suspend) {
            return;
        }
        try {
            try {
                final int now = timeService.getEpochTime();
                if (!peers.hasEnoughConnectedPublicPeers(PeersService.maxNumberOfConnectedPublicPeers)) 
                {
                    List<Future<?>> futures = new ArrayList<>();
                    List<Peer> hallmarkedPeers = peers.getPeers((peer) -> 
                            !peer.isBlacklisted() 
                          && peer.getAnnouncedAddress() != null 
                          && peer.getState() != PeerState.CONNECTED 
                          && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY
                          && peer.providesService(Peer.Service.HALLMARK)
                    );
                    List<Peer> nonhallmarkedPeers = peers.getPeers((peer) -> 
                            !peer.isBlacklisted() 
                          && peer.getAnnouncedAddress() != null 
                          && peer.getState() != PeerState.CONNECTED 
                          && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY
                          && !peer.providesService(Peer.Service.HALLMARK)
                    );
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
                        connectSet.forEach((peer) -> futures.add(peers.peersExecutorService.submit(() -> {
                            PeerAddress pa = new PeerAddress(peer.getAnnouncedAddress());
                            if (peers.isMyAddress(pa)) {
                                return null;
                            }
                            peer.handshake(peers.blockchainConfig.getChain().getChainId());
                            if (peer.getState() == PeerState.CONNECTED
                                    && PeersService.enableHallmarkProtection
                                    && peer.getWeight() == 0
                                    && peers.hasTooManyOutboundConnections())
                            {
                                LOG.debug("Too many outbound connections, deactivating peer " + peer.getHost());
                                peer.deactivate("Too many outbound connections");
                            }
                            return null;
                        })));
                        for (Future<?> future : futures) {
                            future.get();
                        }
                    }
                }
                peers.getPeers(peer ->
                           peer.getState() == PeerState.CONNECTED
                        && now - peer.getLastUpdated() > Constants.PEER_UPDATE_INTERVAL 
                        && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY
                ).forEach((peer) -> {
                        PeerAddress pa = new PeerAddress(peer.getPort(), peer.getHost());
                    if (!peers.isMyAddress(pa)) {
                        peers.peersExecutorService.submit(() -> peer.handshake(peers.blockchainConfig.getChain().getChainId()));
                        }
                });
                if (peers.hasTooManyKnownPeers() && peers.hasEnoughConnectedPublicPeers(peers.maxNumberOfConnectedPublicPeers)) {
                    for (Peer peer : peers.getPeers(peer -> now - peer.getLastUpdated() > Constants.ONE_DAY_SECS)) {
                            peer.remove();
                        if (peers.hasTooFewKnownPeers()) {
                            break;
                        }
                    }
                    if (peers.hasTooManyKnownPeers()) {
                        PriorityQueue<Peer> sortedPeers = new PriorityQueue<>(peers.getAllConnectablePeers());
                        int skipped = 0;
                        while (skipped < PeersService.minNumberOfKnownPeers) {
                            if (sortedPeers.poll() == null) {
                                break;
                            }
                            skipped += 1;
                        }
                        while (!sortedPeers.isEmpty()) {
                            sortedPeers.poll().remove();
                        }
                    }
                }
                for (String wellKnownPeer : peers.wellKnownPeers) {
                    PeerImpl peer = peers.findOrCreatePeer(null, wellKnownPeer, true);
                    if ( peer != null 
                         && now - peer.getLastUpdated() > Constants.PEER_UPDATE_INTERVAL 
                         && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY) {

                        peers.peersExecutorService.submit(() -> {
                            peers.addPeer(peer);
                            peers.connectPeer(peer);
                        });
                    }
                }
            } catch (Exception e) {
                LOG.debug("Error connecting to some peer", e);
            }
        } catch (Throwable t) {
            LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
            //TODO: do we really shoud exit here? Can we recover?
            System.exit(1);
        }
    }
    
}
