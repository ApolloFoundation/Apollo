package com.apollocurrrency.aplwallet.inttest.tests;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.TestBaseNew;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@DisplayName("Sharding")
@Epic(value = "Sharding")
public class TestSharding extends TestBaseNew {


    @Test
    void verifyShards() {
        List<String> peers = TestConfiguration.getTestConfiguration().getPeers();
        HashMap<String, List<ShardDTO>> shards = new HashMap<>();
        HashMap<String, Integer> heights = new HashMap<>();
        List<ShardDTO> maxShardsList = new ArrayList<>();
        int maxHeight = 0;

        for (String ip : peers) {
            try {
                List<ShardDTO> shardDTOS = getShards(ip);
                if (shardDTOS.size() > maxShardsList.size()) {
                    maxShardsList = shardDTOS;
                }
                shards.put(ip, shardDTOS);
                heights.put(ip, getLastBlock(ip).getHeight());
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        maxHeight = heights.values().stream().max(Comparator.comparing(Integer::valueOf)).get();
        int finalMaxHeight = maxHeight;
        List<ShardDTO> finalMaxShardsList = maxShardsList;
        List<String> peersOnCurrentHeight = heights.entrySet().stream().filter(pair -> pair.getValue() > finalMaxHeight - 1000).map(Map.Entry::getKey).collect(Collectors.toList());

        shards.entrySet().stream().filter(pair -> peersOnCurrentHeight.contains(pair.getKey()))
            .filter(pair -> pair.getValue().size() > 0)
            .forEach(pair -> assertEquals("Shards count on: " + pair.getKey(),
                finalMaxShardsList.get(finalMaxShardsList.size() - 1).shardId,
                pair.getValue().get(pair.getValue().size() - 1).getShardId()));

        for (Map.Entry<String, List<ShardDTO>> shard : shards.entrySet()) {
            if (shard.getValue().size() >= finalMaxShardsList.size()) {
                assertIterableEquals(maxShardsList, shard.getValue(), "Assert CoreZip Hash on " + shard.getKey());
            }

        }
    }

}
