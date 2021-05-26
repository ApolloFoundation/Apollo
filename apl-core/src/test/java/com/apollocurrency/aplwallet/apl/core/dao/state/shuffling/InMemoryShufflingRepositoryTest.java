/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryShufflingRepositoryTest extends ShufflingRepositoryTest {

    @Override
    public InMemoryShufflingRepository repository() {
        InMemoryShufflingRepository inMemoryShufflingRepository = new InMemoryShufflingRepository();
        inMemoryShufflingRepository.putAll(std.ALL_SHUFFLINGS);
        return inMemoryShufflingRepository;
    }

    @Test
    void testAnalyzeChangesForUnknownColumn() {
        assertThrows(IllegalArgumentException.class, () -> repository().analyzeChanges("unknown-column", "Prev value", std.SHUFFLING_3_3_APL_REGISTRATION));
    }

    @Test
    void testUpdateColumnsDuringRollback() {
        InMemoryShufflingRepository repository = repository();
        repository.rollback(std.SHUFFLING_4_1_APL_DONE.getHeight());
        Shuffling shuffling = repository.get(std.SHUFFLING_4_1_APL_DONE.getId());
        std.SHUFFLING_4_1_APL_DONE.setLatest(true);
        assertEquals(std.SHUFFLING_4_1_APL_DONE, shuffling);
    }

    @Test
    void testSetColumnForUnknownColumnName() {
        assertThrows(IllegalArgumentException.class, () -> repository().setColumn("unknown", "Value", std.SHUFFLING_3_3_APL_REGISTRATION));
    }

    @Test
    void testGetCopy() {
        Shuffling shufflingCopy = repository().getCopy(std.SHUFFLING_8_1_CURRENCY_PROCESSING.getId());

        assertEquals(std.SHUFFLING_8_1_CURRENCY_PROCESSING, shufflingCopy);
    }
}