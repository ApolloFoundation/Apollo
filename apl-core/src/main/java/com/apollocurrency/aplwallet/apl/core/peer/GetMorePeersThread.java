/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.JSON;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.enterprise.inject.spi.CDI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alukin@gmail.com
 */
class GetMorePeersThread implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GetMorePeersThread.class);
    private EpochTime timeService;
     
    public GetMorePeersThread(EpochTime timeService) {
        this.timeService = timeService;
    }
       
    private final JSONStreamAware getPeersRequest;
    {
        JSONObject request = new JSONObject();
        request.put("requestType", "getPeers");
        request.put("chainId", Peers.blockchainConfig.getChain().getChainId());
        getPeersRequest = JSON.prepareRequest(request);
    }
    private volatile boolean updatedPeer;

    @Override
    public void run() {
        try {
            try {
                if (Peers.hasTooManyKnownPeers()) {
                    return;
                }
                Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
                if (peer == null) {
                    return;
                }
                JSONObject response = peer.send(getPeersRequest, Peers.blockchainConfig.getChain().getChainId(), 10 * 1024 * 1024, false);
                if (response == null) {
                    return;
                }
                JSONArray peers = (JSONArray) response.get("peers");
                Set<String> addedAddresses = new HashSet<>();
                if (peers != null) {
                    JSONArray services = (JSONArray) response.get("services");
                    boolean setServices = services != null && services.size() == peers.size();
                    int now = timeService.getEpochTime();
                    for (int i = 0; i < peers.size(); i++) {
                        String announcedAddress = (String) peers.get(i);
                        PeerImpl newPeer = Peers.findOrCreatePeer(announcedAddress, true);
                        if (newPeer != null) {
                            if (now - newPeer.getLastUpdated() > 24 * 3600) {
                                newPeer.setLastUpdated(now);
                                updatedPeer = true;
                            }
                            if (Peers.addPeer(newPeer) && setServices) {
                                newPeer.setServices(Long.parseUnsignedLong((String) services.get(i)));
                            }
                            addedAddresses.add(announcedAddress);
                            if (Peers.hasTooManyKnownPeers()) {
                                break;
                            }
                        }
                    }
                    if (Peers.savePeers && updatedPeer) {
                        updateSavedPeers();
                        updatedPeer = false;
                    }
                }
                JSONArray myPeers = new JSONArray();
                JSONArray myServices = new JSONArray();
                Peers.getAllPeers().forEach((myPeer) -> {
                    if (!myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null && myPeer.getState() == Peer.State.CONNECTED && myPeer.shareAddress() && !addedAddresses.contains(myPeer.getAnnouncedAddress()) && !myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress())) {
                        myPeers.add(myPeer.getAnnouncedAddress());
                        myServices.add(Long.toUnsignedString(((PeerImpl) myPeer).getServices()));
                    }
                });
                if (myPeers.size() > 0) {
                    JSONObject request = new JSONObject();
                    request.put("requestType", "addPeers");
                    request.put("peers", myPeers);
                    request.put("services", myServices); // Separate array for backwards compatibility
                    request.put("chainId", Peers.blockchainConfig.getChain().getChainId());
                    peer.send(JSON.prepareRequest(request), Peers.blockchainConfig.getChain().getChainId(), 0, false);
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
        UUID chainId = Peers.blockchainConfig.getChain().getChainId();
        Peers.peers.values().forEach((peer) -> {
            if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted() && chainId.equals(peer.getChainId()) && now - peer.getLastUpdated() < 7 * 24 * 3600) {
                currentPeers.put(peer.getAnnouncedAddress(), new PeerDb.Entry(peer.getAnnouncedAddress(), peer.getServices(), peer.getLastUpdated()));
            }
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
