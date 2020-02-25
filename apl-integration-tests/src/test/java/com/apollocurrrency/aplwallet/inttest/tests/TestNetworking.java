package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Networking")
@Epic(value = "Networking")
public class TestNetworking extends TestBaseNew {

    @Test
    @DisplayName("Get Peers")
    public void getAllPeers() {
        assertTrue(super.getPeers().size() > 0, "Verify count of peers");
    }


    @Test
    @DisplayName("Get Peer")
    public void getPeer() {
        List<String> peers = getPeers();
        if (peers.size() > 0) {
            PeerDTO peer = getPeer(String.valueOf(peers.get(0)));
            assertNotNull(peer.getAddress(),"Address: "+peer.getAddress());
            assertNotNull(peer.getChainId(),"ChainId: "+peer.getChainId());
            assertEquals("Apollo", peer.getApplication());
        } else
            fail("Peers not found");
    }

    @Test
    @DisplayName("Get My Info")
    public void addPeer() {
        PeerInfo peer = getMyInfo();
        assertNotNull(peer.getAddress());
    }

}
