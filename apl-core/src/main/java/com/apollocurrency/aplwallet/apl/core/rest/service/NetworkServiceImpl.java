/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerAddress;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see NetworkService
 */
public class NetworkServiceImpl implements NetworkService {
    private final PeersService peers;

    @Inject
    public NetworkServiceImpl(PeersService peers) {
        this.peers = peers;
    }

    @Override
    public Peer findPeerByAddress(String peerAddress){
        return peers.findOrCreatePeer(new PeerAddress(peerAddress), null, false);
    }

    @Override
    public Peer findOrCreatePeerByAddress(String peerAddress){
        return peers.findOrCreatePeer(null, peerAddress, true);
    }

    @Override
    public boolean addPeer(Peer peer, String peerAddress){
        return peers.addPeer(peer, peerAddress);
    }

    @Override
    public List<Peer> getPeersByStateAndService(boolean active, PeerState state, final long services){
        List<Peer> result;
        List<Peer> peersList = active ? peers.getActivePeers() : state != null ? peers.getPeers(state) : new ArrayList<>(peers.getAllPeers());

        if (services != 0) {
            result = peersList.stream().filter(o -> o.providesServices(services)).collect(Collectors.toList());
        }else {
            result = peersList;
        }
        return result;
    }

    @Override
    public List<Peer> getInboundPeers() {
        return peers.getInboundPeers();
    }

    @Override
    public Peer putPeerInBlackList(String peerAddress) {
        Peer peer = peers.findOrCreatePeer(null, peerAddress, true);
        if (peer != null) {
            peers.addPeer(peer);
            peer.blacklist("Manual blacklist");
        }
        return peer;
    }

    @Override
    public boolean putPeerInProxyBlackList(Peer peer){
        return APIProxy.getInstance().blacklistHost(peer.getHost());
    }

    @Override
    public Peer setForcedPeer(Peer peer){
        return APIProxy.getInstance().setForcedPeer(peer);
    }

    @Override
    public List<Peer> getOutboundPeers() {
        return peers.getOutboundPeers();
    }

    @Override
    public PeerInfo getMyPeerInfo() {
        return peers.getMyPeerInfo();
    }
}
