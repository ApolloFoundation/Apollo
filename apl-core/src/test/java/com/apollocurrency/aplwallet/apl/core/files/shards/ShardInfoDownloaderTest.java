/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author al
 */
public class ShardInfoDownloaderTest {
    
    public ShardInfoDownloaderTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of processAllPeersShardingInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testProcessAllPeersShardingInfo() {
        System.out.println("processAllPeersShardingInfo");
        ShardInfoDownloader instance = null;
        instance.processAllPeersShardingInfo();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of processPeerShardingInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testProcessPeerShardingInfo() {
        System.out.println("processPeerShardingInfo");
        String pa = "";
        ShardingInfo si = null;
        ShardInfoDownloader instance = null;
        boolean expResult = false;
        boolean result = instance.processPeerShardingInfo(pa, si);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getShardingInfoFromPeer method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardingInfoFromPeer() {
    }

    /**
     * Test of getShardInfoFromPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardInfoFromPeers() {
    }

    /**
     * Test of getShardInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardInfo() {
        System.out.println("getShardInfo");
        Long shardId = null;
        ShardInfoDownloader instance = null;
        ShardInfo expResult = null;
        ShardInfo result = instance.getShardInfo(shardId);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getShardWeight method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardWeight() {
        System.out.println("getShardWeight");
        Long shardId = null;
        ShardInfoDownloader instance = null;
        double expResult = 0.0;
        double result = instance.getShardWeight(shardId);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getShardRelativeWeights method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardRelativeWeights() {
        System.out.println("getShardRelativeWeights");
        ShardInfoDownloader instance = null;
        Map<Long, Double> expResult = null;
        Map<Long, Double> result = instance.getShardRelativeWeights();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


    /**
     * Test of getShardsPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardsPeers() {
        System.out.println("getShardsPeers");
        ShardInfoDownloader instance = null;
        Map<Long, Set<String>> expResult = null;
        Map<Long, Set<String>> result = instance.getShardsPeers();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getShardsDesisons method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardsDesisons() {
        System.out.println("getShardsDesisons");
        ShardInfoDownloader instance = null;
        Map<Long, FileDownloadDecision> expResult = null;
        Map<Long, FileDownloadDecision> result = instance.getShardsDesisons();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getShardInfoByPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardInfoByPeers() {
        System.out.println("getShardInfoByPeers");
        ShardInfoDownloader instance = null;
        Map<String, ShardingInfo> expResult = null;
        Map<String, ShardingInfo> result = instance.getShardInfoByPeers();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGoodPeersMap method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetGoodPeersMap() {
        System.out.println("getGoodPeersMap");
        ShardInfoDownloader instance = null;
        Map<Long, Set<PeerFileHashSum>> expResult = null;
        Map<Long, Set<PeerFileHashSum>> result = instance.getGoodPeersMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
