/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit test for class used for generating shard name file by specified pattern.
 *
 * @author yuriy.larin
 */
class ShardNameHelperTest {

    @Test
    void getShardName() {
        String result = ShardNameHelper.getShardNameByShardId(001L);
        assertEquals("apl-blockchain-shard-1", result);

        result = ShardNameHelper.getShardNameByShardId(2001L);
        assertEquals("apl-blockchain-shard-2001", result);

        result = ShardNameHelper.getShardNameByShardId(0L);
        assertEquals("apl-blockchain-shard-0", result);
    }

    @Test
    void getShardArchiveName() {
        String result = ShardNameHelper.getShardArchiveNameByShardId(001L);
        assertEquals("apl-blockchain-arch-1", result);

        result = ShardNameHelper.getShardArchiveNameByShardId(2001L);
        assertEquals("apl-blockchain-arch-2001", result);

        result = ShardNameHelper.getShardArchiveNameByShardId(0L);
        assertEquals("apl-blockchain-arch-0", result);
    }

    @Test
    void getShardNameIncorrectValue() {
        // shard name
        assertThrows(RuntimeException.class, () ->
                ShardNameHelper.getShardNameByShardId(null)
        );
        assertThrows(RuntimeException.class, () ->
                ShardNameHelper.getShardNameByShardId(-100L)
        );
        // archive name
        assertThrows(RuntimeException.class, () ->
                ShardNameHelper.getShardArchiveNameByShardId(null)
        );
        assertThrows(RuntimeException.class, () ->
                ShardNameHelper.getShardArchiveNameByShardId(-100L)
        );
    }
}