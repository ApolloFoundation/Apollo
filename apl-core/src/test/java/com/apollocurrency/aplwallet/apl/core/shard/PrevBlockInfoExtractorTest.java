/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.shard.model.PrevBlockData;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("slow")
public class PrevBlockInfoExtractorTest {
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();
    PrevBlockInfoExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PrevBlockInfoExtractor(dbExtension.getDatabaseManager());
    }

    @Test
    void testGetIdsBeforeGenesisHeight() {
        PrevBlockData prevBlockData = extractor.extractPrevBlockData(0, Integer.MAX_VALUE);

        assertEquals(0, prevBlockData.getGeneratorIds().length);
        assertEquals(0, prevBlockData.getPrevBlockTimeouts().length);
        assertEquals(0, prevBlockData.getPrevBlockTimestamps().length);
    }

    @Test
    void testGetIdsBeforeLastBlockHeight() {
        PrevBlockData prevBlockData = extractor.extractPrevBlockData(BlockTestData.BLOCK_10_HEIGHT, 3);

        assertArrayEquals(new Long[]{BlockTestData.BLOCK_9_GENERATOR, BlockTestData.BLOCK_8_GENERATOR, BlockTestData.BLOCK_7_GENERATOR}, prevBlockData.getGeneratorIds());
        assertArrayEquals(new Integer[]{BlockTestData.BLOCK_9_TIMESTAMP, BlockTestData.BLOCK_8_TIMESTAMP, BlockTestData.BLOCK_7_TIMESTAMP}, prevBlockData.getPrevBlockTimestamps());
        assertArrayEquals(new Integer[]{BlockTestData.BLOCK_9_TIMEOUT, BlockTestData.BLOCK_8_TIMEOUT, BlockTestData.BLOCK_7_TIMEOUT}, prevBlockData.getPrevBlockTimeouts());
    }
}
