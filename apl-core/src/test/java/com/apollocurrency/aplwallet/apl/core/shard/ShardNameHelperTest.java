/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Unit test for class used for generating shard name file by specified pattern.
 *
 * @author yuriy.larin
 */
class ShardNameHelperTest {

    private static final UUID chainId=UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5");

    @Test
    void getShardName() {
        ShardNameHelper shardNameHelper = new ShardNameHelper();
        String result = shardNameHelper.getShardNameByShardId(001L,chainId);
        assertEquals("apl-blockchain-shard-1-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5", result);

        result = shardNameHelper.getShardNameByShardId(2001L,chainId);
        assertEquals("apl-blockchain-shard-2001-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5", result);

    }

    @Test
    void getCoreShardArchiveName() {
        ShardNameHelper shardNameHelper = new ShardNameHelper();
        String result = shardNameHelper.getCoreShardArchiveNameByShardId(001L,chainId);
        assertEquals("apl-blockchain-shard-1-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip", result);

        result = shardNameHelper.getCoreShardArchiveNameByShardId(2001L,chainId);
        assertEquals("apl-blockchain-shard-2001-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip", result);
    }
    
    @Test
    void getPrunableShardArchiveName() {
        ShardNameHelper shardNameHelper = new ShardNameHelper();
        String result = shardNameHelper.getPrunableShardArchiveNameByShardId(001L,chainId);
        assertEquals("apl-blockchain-shardprun-1-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip", result);

        result = shardNameHelper.getPrunableShardArchiveNameByShardId(2001L,chainId);
        assertEquals("apl-blockchain-shardprun-2001-chain-b5d7b697-f359-4ce5-a619-fa34b6fb01a5.zip", result);
    }
    
    @Test
    void getShardNameIncorrectValue() {
        // shard name
        ShardNameHelper shardNameHelper = new ShardNameHelper();
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getShardNameByShardId(null,chainId)
        );
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getShardNameByShardId(-100L,chainId)
        );
        // archive name
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getCoreShardArchiveNameByShardId(null,null)
        );
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getCoreShardArchiveNameByShardId(-100L,null)
        );
    }
}