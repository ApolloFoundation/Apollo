/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
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
 * @author al
 */
class PeerConnectingThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PeerConnectingThread.class);
    private final TimeService timeService;
    private final PeersService peersService;

    public PeerConnectingThread(TimeService timeService, PeersService peersService) {
        this.timeService = timeService;
        this.peersService = peersService;
    }

    @Override
    public void run() {
        if (peersService.shutdown || peersService.suspend) {
            return;
        }
        try {
            try {
                final int now = timeService.getEpochTime();
                //connect to configured peers first
                for (String wellKnownPeer : peersService.wellKnownPeers) {
                    Peer peer = peersService.findOrCreatePeer(null, wellKnownPeer, true);
                    if (peer != null
                        // && now - peer.getLastUpdated() > Constants.PEER_UPDATE_INTERVAL
                        && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY) {

                        peersService.peersExecutorService.submit(() -> {
                            peersService.addPeer(peer);
                            peersService.connectPeer(peer);
                        });
                    }
                }
                if (!peersService.hasEnoughConnectedPublicPeers(PeersService.maxNumberOfConnectedPublicPeers)) {
                    List<Future<?>> futures = new ArrayList<>();
                    List<Peer> hallmarkedPeers = peersService.getPeers((peer) ->
                        !peer.isBlacklisted()
                            && peer.getAnnouncedAddress() != null
                            && peer.getState() != PeerState.CONNECTED
                            && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY
                            && peer.providesService(Peer.Service.HALLMARK)
                    );
                    List<Peer> nonhallmarkedPeers = peersService.getPeers((peer) ->
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
                        connectSet.forEach((peer) -> futures.add(peersService.peersExecutorService.submit(() -> {
                            PeerAddress pa = new PeerAddress(peer.getAnnouncedAddress());
                            if (peersService.isMyAddress(pa)) {
                                return null;
                            }
                            peersService.connectPeer(peer);
                            if (peer.getState() == PeerState.CONNECTED
                                && PeersService.enableHallmarkProtection
                                && peer.getWeight() == 0
                                && peersService.hasTooManyOutboundConnections()) {
                                LOG.debug("Too many outbound connections, deactivating peer {}", peer.getHostWithPort());
                                peer.deactivate("Too many outbound connections");
                            }
                            return null;
                        })));
                        for (Future<?> future : futures) {
                            future.get();
                        }
                    }
                }
                peersService.getPeers(peer ->
                    peer.getState() != PeerState.CONNECTED
                        && now - peer.getLastUpdated() > Constants.PEER_UPDATE_INTERVAL
                        && now - peer.getLastConnectAttempt() > Constants.PEER_RECONNECT_ATTMEPT_DELAY
                ).forEach((peer) -> {
                    PeerAddress pa = new PeerAddress(peer.getPort(), peer.getHost());
                    if (!peersService.isMyAddress(pa)) {
                        peersService.peersExecutorService.submit(() -> peersService.connectPeer(peer));
                    }
                });
                if (peersService.hasTooManyKnownPeers() && peersService.hasEnoughConnectedPublicPeers(PeersService.maxNumberOfConnectedPublicPeers)) {
                    for (Peer peer : peersService.getPeers(peer -> now - peer.getLastUpdated() > Constants.ONE_DAY_SECS)) {
                        peer.remove();
                        if (peersService.hasTooFewKnownPeers()) {
                            break;
                        }
                    }
                    if (peersService.hasTooManyKnownPeers()) {
                        PriorityQueue<Peer> sortedPeers = new PriorityQueue<>(peersService.getAllConnectablePeers());
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

            } catch (Exception e) {
                LOG.debug("Error connecting to some peer", e);
            }
        } catch (Throwable t) {
            LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
            //TODO: do we really should exit here? Can we recover?
            System.exit(1);
        }
    }

}
