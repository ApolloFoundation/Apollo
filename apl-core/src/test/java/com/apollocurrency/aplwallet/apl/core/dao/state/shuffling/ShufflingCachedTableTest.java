/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import com.apollocurrency.aplwallet.apl.testutil.MockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ShufflingCachedTableTest {
    ShufflingCachedTable cachedTable;
    @Mock
    InMemoryShufflingRepository inMemRepo;
    @Mock
    ShufflingTable dbTable;
    ShufflingTestData td;

    @BeforeEach
    void setUp() {
        cachedTable = new ShufflingCachedTable(inMemRepo, dbTable);
        td = new ShufflingTestData();
    }

    @Test
    void getActiveCount() {
        doReturn(22).when(inMemRepo).getActiveCount();

        int activeCount = cachedTable.getActiveCount();

        assertEquals(22, activeCount);
    }

    @Test
    void extractAll() {
        List<Shuffling> expected = List.of(td.SHUFFLING_4_1_APL_DONE, td.SHUFFLING_3_3_APL_REGISTRATION);
        doReturn(expected).when(inMemRepo).extractAll(0, -1);

        List<Shuffling> actual = cachedTable.extractAll(0, -1);

        assertEquals(expected, actual);
    }

    @Test
    void getActiveShufflings() {
        doReturn(List.of(td.SHUFFLING_4_1_APL_DONE, td.SHUFFLING_7_1_CURRENCY_DONE)).when(inMemRepo).getActiveShufflings(2, 3);

        List<Shuffling> activeShufflings = cachedTable.getActiveShufflings(2, 3);

        assertEquals(List.of(td.SHUFFLING_4_1_APL_DONE, td.SHUFFLING_7_1_CURRENCY_DONE), activeShufflings);

    }

    @Test
    void getFinishedShufflings() {
        doReturn(List.of(td.SHUFFLING_4_1_APL_DONE, td.SHUFFLING_3_3_APL_REGISTRATION)).when(inMemRepo).getFinishedShufflings(5, 6);

        List<Shuffling> finishedShufflings = cachedTable.getFinishedShufflings(5, 6);

        assertEquals(List.of(td.SHUFFLING_4_1_APL_DONE, td.SHUFFLING_3_3_APL_REGISTRATION), finishedShufflings);
    }

    @Test
    void get() {
        doReturn(td.SHUFFLING_1_2_APL_DONE_DELETED).when(inMemRepo).get(new LongKey(td.SHUFFLING_1_2_APL_DONE_DELETED.getId()));

        Shuffling shuffling = cachedTable.get(new LongKey(td.SHUFFLING_1_2_APL_DONE_DELETED.getId()));

        assertEquals(td.SHUFFLING_1_2_APL_DONE_DELETED, shuffling);
    }

    @Test
    void getHoldingShufflingCount() {
        doReturn(2).when(inMemRepo).getHoldingShufflingCount(0, true);

        int shufflingCount = cachedTable.getHoldingShufflingCount(0, true);

        assertEquals(2, shufflingCount);
    }

    @Test
    void getHoldingShufflings() {
        List<Shuffling> expected = List.of(td.SHUFFLING_4_1_APL_DONE, td.SHUFFLING_3_3_APL_REGISTRATION);
        doReturn(expected).when(inMemRepo).getHoldingShufflings(0, ShufflingStage.BLAME, true, 1,2);

        List<Shuffling> returned = cachedTable.getHoldingShufflings(0, ShufflingStage.BLAME, true, 1, 2);

        assertEquals(expected, returned);
    }

    @Test
    void getAssignedShufflings() {
        List<Shuffling> expected = List.of(td.SHUFFLING_4_1_APL_DONE, td.SHUFFLING_3_3_APL_REGISTRATION);
        doReturn(expected).when(inMemRepo).getAssignedShufflings(100, 1,2);

        List<Shuffling> returned = cachedTable.getAssignedShufflings(100, 1, 2);

        assertEquals(expected, returned);
    }

    @Test
    void delete() {
        doReturn(true).when(inMemRepo).delete(td.SHUFFLING_3_3_APL_REGISTRATION);
        int deletionHeight = td.SHUFFLING_3_3_APL_REGISTRATION.getHeight();
        doReturn(true).when(dbTable).deleteAtHeight(td.SHUFFLING_3_3_APL_REGISTRATION, deletionHeight);
        MockUtils.doAnswer(Map.of(1, 10, 2, 9)).when(inMemRepo).rowCount(deletionHeight);
        MinMaxValue beforeOpMinMaxValue = new MinMaxValue(BigDecimal.ZERO, BigDecimal.valueOf(100), "db_id", 10, deletionHeight);
        MinMaxValue afterOpMinMaxValue = new MinMaxValue(BigDecimal.ZERO, BigDecimal.valueOf(100), "db_id", 9, deletionHeight);
        MockUtils.doAnswer(Map.of(1, beforeOpMinMaxValue, 2, afterOpMinMaxValue, 3, afterOpMinMaxValue)).when(dbTable).getMinMaxValue(deletionHeight);

        boolean deleted = cachedTable.delete(td.SHUFFLING_3_3_APL_REGISTRATION);

        assertTrue(deleted, "Expected deleted = false after cache+db successful delete");
    }

    @Test
    void getAccountShufflings() {
        List<Shuffling> expected = List.of(td.SHUFFLING_4_2_APL_FINISHED, td.SHUFFLING_5_1_APL_PROCESSING);
        doReturn(expected).when(dbTable).getAccountShufflings(1000, true, 10,11);

        List<Shuffling> returned = cachedTable.getAccountShufflings(1000, true, 10, 11);

        assertEquals(expected, returned);
    }
}