package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.*;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TestNetworking extends TestBase {

    @Test
    @DisplayName("Get Peers")
    public void getAllPeers() throws IOException {
        assertTrue(super.getPeers().size() > 0);
    }

    @Test
    @DisplayName("Get Peer")
    public void getPeer() throws IOException {
        PeerDTO peer = getPeer(String.valueOf(getPeers().get(0)));
        assertNotNull(peer.getAddress());
        assertEquals("a2e9b946-290b-48b6-9985-dc2e5a5860a1",peer.getChainId());
        assertEquals("Apollo",peer.getApplication());
    }

    @Test
    @DisplayName("Get My Info")
    public void addPeer() throws IOException {
        PeerInfo peer = getMyInfo();
        assertNotNull(peer.getAddress());
    }



}
