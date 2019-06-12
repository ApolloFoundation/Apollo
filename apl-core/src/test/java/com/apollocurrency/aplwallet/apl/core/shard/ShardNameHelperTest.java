/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for class used for generating shard name file by specified pattern.
 *
 * @author yuriy.larin
 */
class ShardNameHelperTest {
    private static ChainsConfigHolder chainCoinfig;
    private static Chain chain;
    private static final UUID chainId=UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5");

    @BeforeAll
    public void init(){
        chain = mock(Chain.class);
        when(chain.getChainId()).thenReturn(chainId);
        chainCoinfig = mock(ChainsConfigHolder.class);
        when(chainCoinfig.getActiveChain()).thenReturn(chain);
    }
    @Test
    void getShardName() {
        ShardNameHelper shardNameHelper = new ShardNameHelper(chainCoinfig);
        String result = shardNameHelper.getShardNameByShardId(001L,chainId);
        assertEquals("apl-blockchain-shard-1", result);

        result = shardNameHelper.getShardNameByShardId(2001L,null);
        assertEquals("apl-blockchain-shard-2001", result);

        result = shardNameHelper.getShardNameByShardId(0L,null);
        assertEquals("apl-blockchain-shard-0", result);
    }

    @Test
    void getShardArchiveName() {
        ShardNameHelper shardNameHelper = new ShardNameHelper(chainCoinfig);
        String result = shardNameHelper.getShardArchiveNameByShardId(001L,chainId);
        assertEquals("apl-blockchain-arch-1", result);

        result = shardNameHelper.getShardArchiveNameByShardId(2001L,null);
        assertEquals("apl-blockchain-arch-2001", result);

        result = shardNameHelper.getShardArchiveNameByShardId(0L,null);
        assertEquals("apl-blockchain-arch-0", result);
    }

    @Test
    void getShardNameIncorrectValue() {
        // shard name
        ShardNameHelper shardNameHelper = new ShardNameHelper(chainCoinfig);
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getShardNameByShardId(null,null)
        );
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getShardNameByShardId(-100L,null)
        );
        // archive name
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getShardArchiveNameByShardId(null,null)
        );
        assertThrows(RuntimeException.class, () ->
                shardNameHelper.getShardArchiveNameByShardId(-100L,null)
        );
    }
}