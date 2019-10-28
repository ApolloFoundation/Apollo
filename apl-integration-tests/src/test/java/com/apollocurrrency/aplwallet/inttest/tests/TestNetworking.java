package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseOld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class TestNetworking extends TestBaseNew {
//public class TestNetworking extends TestBaseOld {

    @Test
    @DisplayName("Get Peers")
    public void getAllPeers() {
        assertTrue(super.getPeers().size() > 0);
    }


    @Test
    @DisplayName("Get Peer")
    public void getPeer(){
        PeerDTO peer = getPeer(String.valueOf(getPeers().get(0)));
        assertNotNull(peer.getAddress());
        assertNotNull(peer.getChainId());
        assertEquals("Apollo",peer.getApplication());
    }

    @Test
    @DisplayName("Get My Info")
    public void addPeer() {
        PeerInfo peer = getMyInfo();
        assertNotNull(peer.getAddress());
    }

}
