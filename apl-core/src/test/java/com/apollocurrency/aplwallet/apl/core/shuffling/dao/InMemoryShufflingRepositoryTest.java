/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class InMemoryShufflingRepositoryTest {
    private ShufflingTestData std;
    private InMemoryShufflingRepository shufflingRepository;
    @BeforeEach
    void setUp() {
        std = new ShufflingTestData();
        shufflingRepository = new InMemoryShufflingRepository(new ShufflingKeyFactory());
        shufflingRepository.putAll(List.of(std.APL_SHUFFLING1_1, std.APL_SHUFFLING1_2, std.APL_SHUFFLING1_3, std.ASSET_SHUFFLING2_1, std.ASSET_SHUFFLING2_2, std.CURRENCY_SHUFFLING3_1, std.DELETED_SHUFFLING4_1, std.DELETED_SHUFFLING4_2));
    }

    @Test
    void testGetAllActiveShufflings() {
        List<Shuffling> activeShufflings = shufflingRepository.getActiveShufflings(0, Integer.MAX_VALUE);
        List<Shuffling> expected = List.of(std.APL_SHUFFLING1_3,std.ASSET_SHUFFLING2_2, std.CURRENCY_SHUFFLING3_1);

        assertEquals(expected, activeShufflings);
    }

}