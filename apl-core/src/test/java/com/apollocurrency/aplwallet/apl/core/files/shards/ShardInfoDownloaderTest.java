/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.files.shards;

import com.apollocurrency.aplwallet.api.p2p.ShardInfo;
import com.apollocurrency.aplwallet.api.p2p.ShardingInfo;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.PeerFileHashSum;
import com.apollocurrency.aplwallet.apl.core.http.JettyConnectorCreator;
import com.apollocurrency.aplwallet.apl.core.peer.PeerHttpServer;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.task.limiter.TimeLimiterService;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 *
 * @author al
 */
@EnableWeld
public class ShardInfoDownloaderTest {
    
    private static final Map<String,ShardingInfo> shardInfoByPeers_ALL_GOOD = new HashMap<>();
    private static final Map<String,ShardingInfo> shardInfoByPeers_less3rd = new HashMap<>();
    private static final Map<String,ShardingInfo> shardInfoByPeers_3rd50on50 = new HashMap<>();
    private static final Map<String,ShardingInfo> shardInfoByPeers_GOOD_PREV_SHARD = new HashMap<>();
    
    Blockchain blockchain;
    
    public ShardInfoDownloaderTest() {
    }
    
    private static ShardingInfo readData(String fileName){
        String fn = "conf/data/"+fileName;
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = ShardInfoDownloaderTest.class.getClassLoader().getResourceAsStream(fn);
        ShardingInfo res = null;
        try {
            res = mapper.readValue(is, ShardingInfo.class);
        } catch (IOException ex) {
            System.out.println("Can not read file from resources: "+fn);
        }
        return res;
    }
    
    @BeforeAll
    public static void setUpClass() {
        //all 3 shards are OK
        ShardingInfo si1 = readData("shardingTestData1_1.json");
        //3rd shard is bad
        ShardingInfo si2 = readData("shardingTestData1_2.json");
        //3rd and 2nd shards are bad
        ShardingInfo si3 = readData("shardingTestData1_3.json");
        //all 3 shards are bad
        ShardingInfo si4 = readData("shardingTestData1_4.json");
        //no 3rd shard
        ShardingInfo si5 = readData("shardingTestData1_5.json");
        //no 2nd and 3rd shards
        ShardingInfo si6 = readData("shardingTestData1_6.json");
        //not sharding, no shards
        ShardingInfo si7 = new ShardingInfo();
        si7.isShardingOff=true;


        
        //prepare 6 hosts with all 3 good shards and 1 not sharding
        shardInfoByPeers_ALL_GOOD.put("1", si1);
        shardInfoByPeers_ALL_GOOD.put("2", si1);
        shardInfoByPeers_ALL_GOOD.put("3", si1);
        shardInfoByPeers_ALL_GOOD.put("4", si1);
        shardInfoByPeers_ALL_GOOD.put("5", si1);
        shardInfoByPeers_ALL_GOOD.put("6", si1);
        shardInfoByPeers_ALL_GOOD.put("7", si7);
        //prepare 3 hosts with all 3 good shards and 3 with aabsent 3rd and 1 not sharding
        shardInfoByPeers_less3rd.put("1", si1);
        shardInfoByPeers_less3rd.put("2", si1);
        shardInfoByPeers_less3rd.put("3", si1);
        shardInfoByPeers_less3rd.put("4", si5);
        shardInfoByPeers_less3rd.put("5", si5);
        shardInfoByPeers_less3rd.put("6", si5);
        shardInfoByPeers_less3rd.put("7", si7);
        //prepare 3 hosts with all 3 good shards and 3 with bad 2rd and 1 not sharding
        shardInfoByPeers_less3rd.put("1", si1);
        shardInfoByPeers_less3rd.put("2", si1);
        shardInfoByPeers_less3rd.put("3", si1);
        shardInfoByPeers_less3rd.put("4", si2);
        shardInfoByPeers_less3rd.put("5", si2);
        shardInfoByPeers_less3rd.put("6", si2);
        shardInfoByPeers_less3rd.put("7", si7);
        
    }
    
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
                           BlockchainConfig.class,
                           PropertiesHolder.class
                         )
            .addBeans(MockBean.of(blockchain, Blockchain.class))
            .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(PeerHttpServer.class), PeerHttpServer.class))
            .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
            .addBeans(MockBean.of(mock(TimeLimiterService.class), TimeLimiterService.class))            
            .addBeans(MockBean.of(mock(UPnP.class), UPnP.class))            
            .addBeans(MockBean.of(mock(PeersService.class), PeersService.class))            
            .addBeans(MockBean.of(mock(JettyConnectorCreator.class), JettyConnectorCreator.class))            
            .build();
    
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
