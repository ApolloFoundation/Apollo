package com.apollocurrrency.aplwallet.inttest.tests;


import com.apollocurrency.aplwallet.api.dto.Peer;
import com.apollocurrency.aplwallet.api.dto.PeerInfo;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class TestNetworking extends TestBase {

    @Test
    @DisplayName("Get Peers")
    public void getAllPeers() throws IOException {
        assertTrue(super.getPeers().length >0);
    }

    @Test
    @DisplayName("Get Peer")
    public void getPeer() throws IOException {
        Peer peer = getPeer(getPeers()[0]);
        assertNotNull(peer.getAddress());
        assertEquals("a2e9b946-290b-48b6-9985-dc2e5a5860a1",peer.getChainId());
        assertEquals("Apollo",peer.getApplication());
    }

    @Test
    @DisplayName("Get My Info")
    public void addPeer() throws IOException {
        PeerInfo peer = getMyInfo();
        assertNotNull(peer.address);
        assertNotNull(peer.host);
    }



}
