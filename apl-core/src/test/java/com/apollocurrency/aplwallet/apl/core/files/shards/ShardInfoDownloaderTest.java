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
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author alukin@gmailcom
 */
@Slf4j
public class ShardInfoDownloaderTest {

    private static final Map<String, ShardingInfo> shardInfoByPeers_ALL_GOOD = new HashMap<>();
    private static final Map<String, ShardingInfo> shardInfoByPeers_less3rd = new HashMap<>();
    private static final Map<String, ShardingInfo> shardInfoByPeers_3rd50on50 = new HashMap<>();
    private static final Map<String, ShardingInfo> shardInfoByPeers_GOOD_PREV_SHARD = new HashMap<>();

    private final PeersService peersService = mock(PeersService.class);

    private final Chain chain = mock(Chain.class);
    private final BlockchainConfig blockchainConfig = spy(new BlockchainConfig());

    private ShardInfoDownloader downloader;

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
        si7.isShardingOff = true;


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

    private static ShardingInfo readData(String fileName) {
        String fn = "conf/data/" + fileName;
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = ShardInfoDownloaderTest.class.getClassLoader().getResourceAsStream(fn);
        ShardingInfo res = null;
        try {
            res = mapper.readValue(is, ShardingInfo.class);
        } catch (IOException ex) {
            System.out.println("Can not read file from resources: " + fn);
        }
        return res;
    }
    /*
     *  TODO: finish tests, there is a lot to test yet
     *
     */

    @BeforeEach
    void setUp() {
        doReturn(UUID.fromString("d5c22b16-935e-495d-aa3f-bb26ef2115d3")).when(chain).getChainId();
        doReturn(chain).when(blockchainConfig).getChain();

        doReturn(null).when(peersService).getAllConnectablePeers();
        doReturn(null).when(peersService).findOrCreatePeer(null, "ADR1", true);
        doReturn(null).when(peersService).findOrCreatePeer(null, "ADR2", true);
        doReturn(blockchainConfig).when(peersService).getBlockchainConfig();
        downloader = new ShardInfoDownloader(peersService);
    }

    /**
     * Test of processAllPeersShardingInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testProcessAllPeersShardingInfo() {
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        int size = downloader.getGoodPeersMap().size();
        assertEquals(3, size);
    }

    /**
     * Test of processPeerShardingInfo method, of class ShardInfoDownloader.
     */
    @Test
    public void testProcessPeerShardingInfo() {
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        String pa = "1";
        ShardingInfo si = shardInfoByPeers_ALL_GOOD.get(pa);
        boolean expResult = true;
        boolean result = downloader.processPeerShardingInfo(pa, si);
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
        Long shardId = 1L;
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        ShardInfo expResult = shardInfoByPeers_ALL_GOOD.get("1").getShards().get(2);
        ShardInfo result = downloader.getShardInfo(shardId);
        assertEquals(expResult, result);
    }

    /**
     * Test of getShardWeight method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardWeight() {

        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();

        double expResult = 0.7;
        for (long i = 1; i <= downloader.getShardsDesisons().size(); i++) {
            double result = downloader.getShardWeight(i);
            assertEquals(result > expResult, true);
            log.debug("Shard: {} weight: {}", i, result);
        }
    }

    /**
     * Test of getShardRelativeWeights method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardRelativeWeights() {
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        Map<Long, Double> result = downloader.getShardRelativeWeights();
        int size = result.size();
        result.keySet().forEach((id) -> {
            log.debug("Shard: {} RelWeight: {}", id, result.get(id));
        });
        assertEquals(size, 3);
    }

    /**
     * Test of getShardsPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardsPeers() {
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        Map<Long, Set<String>> result = downloader.getShardsPeers();
        int size = result.get(3L).size();
        assertEquals(size, 6);
    }

    /**
     * Test of getShardsDesisons method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardsDesisons() {
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        Map<Long, FileDownloadDecision> result = downloader.getShardsDesisons();
        int size = result.size();
        assertEquals(size, 3);
    }

    /**
     * Test of getShardInfoByPeers method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetShardInfoByPeers() {
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        Map<String, ShardingInfo> result = downloader.getShardInfoByPeers();
        int size = result.size();
        //one gets removed
        assertEquals(size, 6);
    }

    /**
     * Test of getGoodPeersMap method, of class ShardInfoDownloader.
     */
    @Test
    public void testGetGoodPeersMap() {
        downloader.setShardInfoByPeers(shardInfoByPeers_ALL_GOOD);
        downloader.processAllPeersShardingInfo();
        Map<Long, Set<PeerFileHashSum>> result = downloader.getGoodPeersMap();
        int size = result.get(3L).size();
        assertEquals(size, 6);
    }
}
