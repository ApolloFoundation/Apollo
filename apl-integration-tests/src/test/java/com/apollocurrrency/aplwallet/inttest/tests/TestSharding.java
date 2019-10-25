package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import org.bouncycastle.crypto.tls.MACAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestSharding extends TestBaseNew {


    @Test
    void verifyShards(){
     List<String> peers = TestConfiguration.getTestConfiguration().getPeers();
        HashMap<String, List<ShardDTO>> shards = new HashMap<>();
        List<ShardDTO> maxShardsList = new ArrayList<>();
        for (String ip: peers) {
                List<ShardDTO> shardDTOS = getShards(ip);
                if (shardDTOS.size() > maxShardsList.size()){maxShardsList =shardDTOS;}
                shards.put(ip, shardDTOS);
        }

        for (int i = 0; i < maxShardsList.size() ; i++) {
            int finalI = i;
            List<ShardDTO> finalMaxShardsList = maxShardsList;

            //shards.values().stream().filter(shardDTOS -> shardDTOS.size() >= finalMaxShardsList.size()).collect(Collectors.toList()).forEach(e ->System.out.println(e.size()));
            assertTrue("Assert CoreZip Hash",
                    shards.values()
                    .stream().filter(shardDTOS -> shardDTOS.size() >= finalMaxShardsList.size()).collect(Collectors.toList()).stream()
                    .allMatch(pair -> pair.get(finalI).getCoreZipHash().equals(finalMaxShardsList.get(finalI).getCoreZipHash())));
        }

        System.out.println(shards);
    }


}
