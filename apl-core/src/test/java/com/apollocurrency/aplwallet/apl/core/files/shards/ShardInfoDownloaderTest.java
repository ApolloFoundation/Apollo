/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author al
 */
public class ShardInfoDownloaderTest {
    
    private static final Map<String,ShardingInfo> shardInfoByPeers_ALL_GOOD = new HashMap<>();
    private static final Map<String,ShardingInfo> shardInfoByPeers_80 = new HashMap<>();
    private static final Map<String,ShardingInfo> shardInfoByPeers_50 = new HashMap<>();
    private static final Map<String,ShardingInfo> shardInfoByPeers_GOOD_PREV_SHARD = new HashMap<>();
    
    public ShardInfoDownloaderTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
        ShardingInfo si1 = new ShardingInfo();
        ShardingInfo si2 = new ShardingInfo();
        ShardingInfo si3 = new ShardingInfo();
        ShardingInfo si4 = new ShardingInfo();
        ShardingInfo si5 = new ShardingInfo();
        ShardingInfo si6 = new ShardingInfo();
        ShardingInfo si7 = new ShardingInfo();

        ShardInfo s1 = new ShardInfo();
        ShardInfo s2 = new ShardInfo();
        ShardInfo s3 = new ShardInfo();
        ShardInfo s1_b = new ShardInfo();
        ShardInfo s2_b = new ShardInfo();
        ShardInfo s3_b = new ShardInfo();
 
//si 1 - all 3 good shards
        si1.shards.add(s1);
        si1.shards.add(s2);
        si1.shards.add(s3);
        si1.isShardingOff=false;
        
        //prepare 6 hosts with all 3 good shards
        shardInfoByPeers_ALL_GOOD.put("1", si1);
        shardInfoByPeers_ALL_GOOD.put("2", si1);
        shardInfoByPeers_ALL_GOOD.put("3", si1);
        shardInfoByPeers_ALL_GOOD.put("4", si1);
        shardInfoByPeers_ALL_GOOD.put("5", si1);
        shardInfoByPeers_ALL_GOOD.put("6", si1);
        
    }
    
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
                           PeersService.class,
                           BlockchainConfig.class
                         ).build();
    
    @Inject
    ShardInfoDownloader instance;
    /**
     * Test of processAllPeersShardingInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testProcessAllPeersShardingInfo() {
        System.out.println("processAllPeersShardingInfo");
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();
        assertEquals(instance.getGoodPeersMap().size(),3);
    }

    /**
     * Test of processPeerShardingInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testProcessPeerShardingInfo() {
        System.out.println("processPeerShardingInfo");
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();        
        String pa = "1";
        ShardingInfo si = shardInfoByPeers_ALL_GOOD.get(pa);
        boolean expResult = true;
        boolean result = instance.processPeerShardingInfo(pa, si);
        assertEquals(expResult, result);
    }

    /**
     * Test of getShardingInfoFromPeer method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardingInfoFromPeer() {
        //should be nothing here, data are prepared
    }

    /**
     * Test of getShardInfoFromPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardInfoFromPeers() {
        //should be nothing here, data are prepared        
    }

    /**
     * Test of getShardInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardInfo() {
        System.out.println("getShardInfo");
        Long shardId = 1L;
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();
        ShardInfo expResult = shardInfoByPeers_ALL_GOOD.get("1").getShards().get(0);
        ShardInfo result = instance.getShardInfo(shardId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getShardWeight method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardWeight() {
        System.out.println("getShardWeight");
        Long shardId = 3L;
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();        

        double expResult = 0.0;
        double result = instance.getShardWeight(shardId);
        assertTrue(expResult==result);
    }

    /**
     * Test of getShardRelativeWeights method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardRelativeWeights() {
        System.out.println("getShardRelativeWeights");
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();        
        Map<Long, Double> result = instance.getShardRelativeWeights();

    }


    /**
     * Test of getShardsPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardsPeers() {
        System.out.println("getShardsPeers");
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();         

        Map<Long, Set<String>> result = instance.getShardsPeers();
    }

    /**
     * Test of getShardsDesisons method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardsDesisons() {
        System.out.println("getShardsDesisons");
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();         
        Map<Long, FileDownloadDecision> result = instance.getShardsDesisons();
    }

    /**
     * Test of getShardInfoByPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardInfoByPeers() {
        System.out.println("getShardInfoByPeers");
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo(); 
        Map<String, ShardingInfo> result = instance.getShardInfoByPeers();
    }

    /**
     * Test of getGoodPeersMap method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetGoodPeersMap() {
        System.out.println("getGoodPeersMap");
        instance.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        instance.processAllPeersShardingInfo();         
        Map<Long, Set<PeerFileHashSum>> result = instance.getGoodPeersMap();
    }
    
}
