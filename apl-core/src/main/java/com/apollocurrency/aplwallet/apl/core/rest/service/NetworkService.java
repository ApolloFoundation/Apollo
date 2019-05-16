/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.Peers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkService {

    public Peer findPeerByAddress(String peerAddress){
        return Peers.findOrCreatePeer(peerAddress, false);
    }

    public List<Peer> getPeersByStateAndService(boolean active, Peer.State state, final long services){
        List<Peer> result;
        List<Peer> peers = active ? Peers.getActivePeers() : state != null ? Peers.getPeers(state) : new ArrayList<>(Peers.getAllPeers());

        if (services != 0) {
            result = peers.stream().filter(o -> o.providesServices(services)).collect(Collectors.toList());
        }else {
            result = peers;
        }
        return result;
    }

}
