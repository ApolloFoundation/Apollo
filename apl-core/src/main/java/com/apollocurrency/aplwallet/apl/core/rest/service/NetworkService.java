/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;

import java.util.List;

public interface NetworkService {

    /**
     * Get peer by ip address.
     * @param peerAddress peer address
     * @return The peer or <i>null</i>
     */
    Peer findPeerByAddress(String peerAddress);

    /**
     * Get peer by ip address.
     * @param peerAddress peer address
     * @return The peer or <code>null</code> if peer can't be created.
     */
    Peer findOrCreatePeerByAddress(String peerAddress);

    /**
     * Add the peer
     * @param peer peer
     * @param peerAddress peer address
     * @return <code>true</code> if peer was successfully added.
     */
    boolean addPeer(Peer peer, String peerAddress);

    /**
     * Get peer list by proposed parameters.
     * @param active include Active peers only
     * @param state include peers with given state.
     * @param services filter peers by services mask
     * @return The peers list or empty list if there aren't suitable by requirements peers.
     */
    List<Peer> getPeersByStateAndService(boolean active, PeerState state, long services);

    /**
     * Returns a list of inbound peers.
     * An inbound peer is a peer that has sent a request to this peer within the previous 30 minutes.
     * @return The inbound peers list.
     */
    List<Peer> getInboundPeers();


    /**
     * Put the peer in the blacklist
     * @param peerAddress peer address
     * @return the peer corresponded the peerAddress.
     */
    Peer putPeerInBlackList(String peerAddress);

    /**
     * Put the peer in the API proxy blacklist
     * @param peer
     * @return
     */
    boolean putPeerInProxyBlackList(Peer peer);

    /**
     * Set the forced peer
     * @param peer peer
     * @return the forced peer
     */
    Peer setForcedPeer(Peer peer);

    /**
     * Get list of peers we're connected to
     *
     * @return list of outbound  peers
     */
    public List<Peer> getOutboundPeers();

    /**
     * Get info amout my peer
     *
     * @return peer info of this peer
     */

    public PeerInfo getMyPeerInfo();
}
