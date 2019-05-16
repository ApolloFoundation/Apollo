/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;

import java.util.List;

public interface NetworkService {

    /**
     * Get peer by ip address.
     * @param peerAddress peer address
     * @return The peer or <i>null</i>
     */
    Peer findPeerByAddress(String peerAddress);

    Peer findOrCreatePeerByAddress(String peerAddress);

    boolean addPeer(Peer peer, String peerAddress);

    /**
     * Get peer list by proposed parameters.
     * @param active include Active peers only
     * @param state include peers with given state.
     * @param services filter peers by services mask
     * @return The peers list or empty list if there aren't suitable by requirements peers.
     */
    List<Peer> getPeersByStateAndService(boolean active, Peer.State state, long services);

    /**
     * Returns a list of inbound peers.
     * An inbound peer is a peer that has sent a request to this peer within the previous 30 minutes.
     * @return The inbound peers list.
     */
    List<Peer> getInboundPeers();


    Peer putPeerInBlackList(String peerAddress);

    boolean putAPIProxyPeerInBlackList(Peer peer);

    Peer setForcedPeer(Peer peer);
}
