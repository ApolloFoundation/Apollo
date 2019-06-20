/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class GeneratorIdsExtractorTest {
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();
    GeneratorIdsExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new GeneratorIdsExtractor(dbExtension.getDatabaseManager());
    }

    @Test
    void testGetIdsBeforeGenesisHeight() {
        List<Long> generatorIds = extractor.extractGeneratorIdsBefore(0, Integer.MAX_VALUE);

        assertTrue(generatorIds.isEmpty());
    }

    @Test
    void testGetIdsBeforeLastBlockHeight() {
        List<Long> generatorIds = extractor.extractGeneratorIdsBefore(BlockTestData.BLOCK_10_HEIGHT, 3);

        assertEquals(List.of(BlockTestData.BLOCK_9_GENERATOR, BlockTestData.BLOCK_8_GENERATOR, BlockTestData.BLOCK_7_GENERATOR), generatorIds);
    }
}
