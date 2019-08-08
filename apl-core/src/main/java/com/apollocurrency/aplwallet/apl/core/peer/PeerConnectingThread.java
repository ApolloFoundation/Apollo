/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.util.Constants;
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
    private final TimeService timeService;
     
    public PeerConnectingThread(TimeService timeService) {
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
                if (!Peers.hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) 
                {
                    List<Future<?>> futures = new ArrayList<>();
                    List<Peer> hallmarkedPeers = Peers.getPeers((peer) -> 
                            !peer.isBlacklisted() 
                          && peer.getAnnouncedAddress() != null 
                          && peer.getState() != PeerState.CONNECTED 
                          && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY
                          && peer.providesService(Peer.Service.HALLMARK)
                    );
                    List<Peer> nonhallmarkedPeers = Peers.getPeers((peer) -> 
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
                        connectSet.forEach((peer) -> futures.add(Peers.peersExecutorService.submit(() -> {
                            PeerAddress pa = new PeerAddress(peer.getAnnouncedAddress());
                            if(Peers.isMyAddress(pa)){
                                return null;
                            }
                            peer.handshake(Peers.blockchainConfig.getChain().getChainId());
                            if (peer.getState() == PeerState.CONNECTED 
                                && Peers.enableHallmarkProtection 
                                && peer.getWeight() == 0 
                                && Peers.hasTooManyOutboundConnections()) 
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
                Peers.getPeers( peer ->
                           peer.getState() == PeerState.CONNECTED
                        && now - peer.getLastUpdated() > Constants.PEER_UPDATE_INTERVAL 
                        && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY
                ).forEach((peer) -> {
                        PeerAddress pa = new PeerAddress(peer.getPort(), peer.getHost());
                        if (!Peers.isMyAddress(pa)) {
                            Peers.peersExecutorService.submit(() -> peer.handshake(Peers.blockchainConfig.getChain().getChainId()));
                        }
                });
                if (Peers.hasTooManyKnownPeers() && Peers.hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                    for (Peer peer : Peers.getPeers(peer->now - peer.getLastUpdated() > Constants.ONE_DAY_SECS)) {
                            peer.remove();
                        if (Peers.hasTooFewKnownPeers()) {
                            break;
                        }
                    }
                    if (Peers.hasTooManyKnownPeers()) {
                        PriorityQueue<Peer> sortedPeers = new PriorityQueue<>(Peers.getAllConnectablePeers());
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
                }
                for (String wellKnownPeer : Peers.wellKnownPeers) {
                    PeerImpl peer = Peers.findOrCreatePeer(null, wellKnownPeer, true);
                    if ( peer != null 
                         && now - peer.getLastUpdated() > Constants.PEER_UPDATE_INTERVAL 
                         && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY) {
                        
                        Peers.peersExecutorService.submit(() -> {
                                Peers.addPeer(peer);
                                Peers.connectPeer(peer);
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
