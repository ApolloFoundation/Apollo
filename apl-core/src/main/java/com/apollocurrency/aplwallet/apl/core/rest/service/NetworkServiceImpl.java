/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerAddress;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see NetworkService
 */
public class NetworkServiceImpl implements NetworkService {

    @Override
    public Peer findPeerByAddress(String peerAddress){
        return Peers.findOrCreatePeer(new PeerAddress(peerAddress),null, false);
    }

    @Override
    public Peer findOrCreatePeerByAddress(String peerAddress){
        return Peers.findOrCreatePeer(null,peerAddress, true);
    }

    @Override
    public boolean addPeer(Peer peer, String peerAddress){
        return Peers.addPeer(peer, peerAddress);
    }

    @Override
    public List<Peer> getPeersByStateAndService(boolean active, PeerState state, final long services){
        List<Peer> result;
        List<Peer> peers = active ? Peers.getActivePeers() : state != null ? Peers.getPeers(state) : new ArrayList<>(Peers.getAllPeers());

        if (services != 0) {
            result = peers.stream().filter(o -> o.providesServices(services)).collect(Collectors.toList());
        }else {
            result = peers;
        }
        return result;
    }

    @Override
    public List<Peer> getInboundPeers() {
        return Peers.getInboundPeers();
    }

    @Override
    public Peer putPeerInBlackList(String peerAddress) {
        Peer peer = Peers.findOrCreatePeer(null,peerAddress, true);
        if (peer != null) {
            Peers.addPeer(peer);
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
}
