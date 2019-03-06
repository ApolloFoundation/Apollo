package com.apollocurrency.aplwallet.apl.core.app;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShardNameHelperTest {

    @Test
    void getShardName() {
        String result = ShardNameHelper.getShardNameByShardId(001L);
        assertEquals("apl-blockchain-shard-0000001", result);

        result = ShardNameHelper.getShardNameByShardId(2001L);
        assertEquals("apl-blockchain-shard-0002001", result);
    }
}