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
        assertEquals("apl-blockchain-shard-0000001", result);

        result = ShardNameHelper.getShardNameByShardId(2001L);
        assertEquals("apl-blockchain-shard-0002001", result);

        result = ShardNameHelper.getShardNameByShardId(0L);
        assertEquals("apl-blockchain-shard-0000000", result);

    }

    @Test
    void getShardNameIncorrectValue() {
        assertThrows(RuntimeException.class, () ->
                ShardNameHelper.getShardNameByShardId(null)
        );
        assertThrows(RuntimeException.class, () ->
                ShardNameHelper.getShardNameByShardId(-100L)
        );
    }
}