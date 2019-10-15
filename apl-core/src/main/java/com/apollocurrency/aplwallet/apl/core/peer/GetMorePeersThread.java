/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
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
 *
 * @author alukin@gmail.com
 */
class GetMorePeersThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GetMorePeersThread.class);
    private final TimeService timeService;
    private final PeersService peers;
    private final JSONStreamAware getPeersRequest;

    public GetMorePeersThread(TimeService timeService, PeersService peers) {
        this.timeService = timeService;
        this.peers = peers;
        JSONObject request = new JSONObject();
        request.put("requestType", "getPeers");
        request.put("chainId", peers.blockchainConfig.getChain().getChainId());
        getPeersRequest = JSON.prepareRequest(request);        
    }


    private volatile boolean updatedPeer;

    @Override
    public void run() {
        try {
            try {
                if (peers.hasTooManyKnownPeers()) {
                    return;
                }
                Peer peer = peers.getAnyPeer(PeerState.CONNECTED, true);
                if (peer == null) {
                    return;
                }
                JSONObject response = peer.send(getPeersRequest, peers.blockchainConfig.getChain().getChainId());
                if (response == null) {
                    return;
                }
                JSONArray peersArray = (JSONArray) response.get("peers");
                Set<String> addedAddresses = new HashSet<>();
                if (peersArray != null) {
                    JSONArray services = (JSONArray) response.get("services");
                    boolean setServices = services != null && services.size() == peersArray.size();
                    int now = timeService.getEpochTime();
                    for (int i = 0; i < peersArray.size(); i++) {
                        String announcedAddress = (String) peersArray.get(i);
                        PeerImpl newPeer = peers.findOrCreatePeer(null, announcedAddress, true);
                        if (newPeer != null) {
                            if (now - newPeer.getLastUpdated() > 24 * 3600) {
                                newPeer.setLastUpdated(now);
                                updatedPeer = true;
                            }
                            if (peers.addPeer(newPeer) && setServices) {
                                newPeer.setServices(Long.parseUnsignedLong((String) services.get(i)));
                            }
                            addedAddresses.add(announcedAddress);
                            if (peers.hasTooManyKnownPeers()) {
                                break;
                            }
                        }
                    }
                    if (PeersService.savePeers && updatedPeer) {
                        updateSavedPeers();
                        updatedPeer = false;
                    }
                }
                JSONArray myPeers = new JSONArray();
                JSONArray myServices = new JSONArray();
                peers.getAllConnectablePeers().forEach((myPeer) -> {
                    if (!myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null && myPeer.getState() == PeerState.CONNECTED && myPeer.shareAddress() && !addedAddresses.contains(myPeer.getAnnouncedAddress()) && !myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())) {
                        myPeers.add(myPeer.getAnnouncedAddress());
                        myServices.add(Long.toUnsignedString(((PeerImpl) myPeer).getServices()));
                    }
                });
                if (myPeers.size() > 0) {
                    JSONObject request = new JSONObject();
                    request.put("requestType", "addPeers");
                    request.put("peers", myPeers);
                    request.put("services", myServices); // Separate array for backwards compatibility
                    request.put("chainId", peers.blockchainConfig.getChain().getChainId());
                    peer.send(JSON.prepareRequest(request), peers.blockchainConfig.getChain().getChainId());
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
        List<PeerDb.Entry> oldPeers = PeerDb.loadPeers();
        Map<String, PeerDb.Entry> oldMap = new HashMap<>(oldPeers.size());
        oldPeers.forEach((entry) -> oldMap.put(entry.getAddress(), entry));
        //
        // Create the current peer map (note that there can be duplicate peer entries with
        // the same announced address)
        //
        Map<String, PeerDb.Entry> currentPeers = new HashMap<>();
        UUID chainId = peers.blockchainConfig.getChain().getChainId();
        peers.getPeers(
                peer->peer.getAnnouncedAddress()!= null
             && !peer.isBlacklisted() 
             && chainId.equals(peer.getChainId()) 
             && now - peer.getLastUpdated() < 7 * 24 * 3600 
        ).forEach((peer) -> {
            currentPeers.put(peer.getAnnouncedAddress(), new PeerDb.Entry(peer.getAnnouncedAddress(), peer.getServices(), peer.getLastUpdated()));
        });
        //
        // Build toDelete and toUpdate lists
        //
        List<PeerDb.Entry> toDelete = new ArrayList<>(oldPeers.size());
        oldPeers.forEach((entry) -> {
            if (currentPeers.get(entry.getAddress()) == null) {
                toDelete.add(entry);
            }
        });
        List<PeerDb.Entry> toUpdate = new ArrayList<>(currentPeers.size());
        currentPeers.values().forEach((entry) -> {
            PeerDb.Entry oldEntry = oldMap.get(entry.getAddress());
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
            PeerDb.deletePeers(toDelete);
            PeerDb.updatePeers(toUpdate);
        } catch (Exception e) {
            throw e;
        }
    }
    
}
