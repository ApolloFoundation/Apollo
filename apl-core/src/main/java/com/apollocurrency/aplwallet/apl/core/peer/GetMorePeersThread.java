/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.PeerDao;
import com.apollocurrency.aplwallet.api.p2p.request.AddPeersRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetPeersRequest;
import com.apollocurrency.aplwallet.api.p2p.respons.GetPeersResponse;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.PeerEntity;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetPeersResponseParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author alukin@gmail.com
 */
@Slf4j
class GetMorePeersThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GetMorePeersThread.class);
    private final TimeService timeService;
    private final PeersService peersService;
    private final GetPeersRequest getPeersRequest;
    private volatile boolean updatedPeer;
    private final PeerDao peerDao;

    public GetMorePeersThread(TimeService timeService, PeersService peersService) {
        this.timeService = timeService;
        this.peersService = peersService;
        this.peerDao = peersService.getPeerDao();
        getPeersRequest = new GetPeersRequest(peersService.blockchainConfig.getChain().getChainId());
    }

    @Override
    public void run() {
        try {
            try {
                if (peersService.hasTooManyKnownPeers()) {
                    return;
                }
                Peer peer = peersService.getAnyPeer(PeerState.CONNECTED, true);
                if (peer == null) {
                    return;
                }
                GetPeersResponse response = peer.send(getPeersRequest, new GetPeersResponseParser());
                if (response == null) {
                    return;
                }
                List<String> peers = response.getPeers();
                Set<String> addedAddresses = new HashSet<>();
                if (peers != null && !peers.isEmpty()) {
                    List<String> services = response.getServices();
                    boolean setServices = services != null && services.size() == peers.size();
                    int now = timeService.getEpochTime();
                    for (int i = 0; i < peers.size(); i++) {
                        String announcedAddress = peers.get(i);
                        Peer newPeer = peersService.findOrCreatePeer(null, announcedAddress, true);
                        if (newPeer != null) {
                            if (now - newPeer.getLastUpdated() > 24 * 3600) {
                                newPeer.setLastUpdated(now);
                                updatedPeer = true;
                            }
                            if (peersService.addPeer(newPeer) && setServices) {
                                newPeer.setServices(Long.parseUnsignedLong(services.get(i)));
                            }
                            addedAddresses.add(announcedAddress);
                            if (peersService.hasTooManyKnownPeers()) {
                                break;
                            }
                        }
                    }
                    if (peersService.savePeers && updatedPeer) {
                        updateSavedPeers();
                        updatedPeer = false;
                    }
                }
                List<String> myPeers = new ArrayList<>();
                List<String> myServices = new ArrayList<>();
                peersService.getAllConnectablePeers().forEach((myPeer) -> {
                    if (!myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null && myPeer.getState() == PeerState.CONNECTED && myPeer.shareAddress() && !addedAddresses.contains(myPeer.getAnnouncedAddress()) && !myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())) {
                        myPeers.add(myPeer.getAnnouncedAddress());
                        myServices.add(Long.toUnsignedString((myPeer).getServices()));
                    }
                });
                if (myPeers.size() > 0) {
                    AddPeersRequest request = new AddPeersRequest(
                        myPeers, myServices, peersService.blockchainConfig.getChain().getChainId()
                    );

                    if (peer.getState() != PeerState.CONNECTED) {
                        peersService.connectPeer(peer);
                    }
                    if (peer.getState() == PeerState.CONNECTED) {
                        peer.send(request);
                    }
                }
            } catch (Exception e) {
                LOG.debug("Error requesting peers from a peer", e);
            }
        } catch (Throwable t) {
            LOG.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS", t);
            //TODO: can we recover here? Should we really exit?
            System.exit(1);
        }
    }

    private void updateSavedPeers() {
        int now = timeService.getEpochTime();
        //
        // Load the current database entries and map announced address to database entry
        //
        List<PeerEntity> oldPeers = peerDao.loadPeers();
        Map<String, PeerEntity> oldMap = new HashMap<>(oldPeers.size());
        oldPeers.forEach((entry) -> oldMap.put(entry.getAddress(), entry));
        //
        // Create the current peer map (note that there can be duplicate peer entries with
        // the same announced address)
        //
        Map<String, PeerEntity> currentPeers = new HashMap<>();
        UUID chainId = peersService.blockchainConfig.getChain().getChainId();
        peersService.getPeers(
            peer -> peer.getAnnouncedAddress() != null
                && !peer.isBlacklisted()
                && chainId.equals(peer.getChainId())
                && now - peer.getLastUpdated() < 7 * 24 * 3600
        ).forEach((peer) -> {
            currentPeers.put(peer.getIdentity(), new PeerEntity(peer.getAnnouncedAddress(), peer.getServices(), peer.getLastUpdated(),peer.getX509pem(),peer.getHostWithPort()));
        });
        //
        // Build toDelete and toUpdate lists
        //
        List<PeerEntity> toDelete = new ArrayList<>(oldPeers.size());
        oldPeers.forEach((entry) -> {
            if (currentPeers.get(entry.getAddress()) == null) {
                toDelete.add(entry);
            }
        });
        List<PeerEntity> toUpdate = new ArrayList<>(currentPeers.size());
        currentPeers.values().forEach((entry) -> {
            PeerEntity oldEntry = oldMap.get(entry.getAddress());
            if (oldEntry == null || entry.getLastUpdated() - oldEntry.getLastUpdated() > 24 * 3600) {
                toUpdate.add(entry);
            }
        });
        //
        // Nothing to do if all of the lists are empty
        //
        if (toDelete.isEmpty() && toUpdate.isEmpty()) {
            return;
        }
        //
        // Update the peer database
        //

        try {
            peerDao.deletePeers(toDelete);
            peerDao.updatePeers(toUpdate);
        } catch (Exception e) {
            throw e;
        }
    }

}
