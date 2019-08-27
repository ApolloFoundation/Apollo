/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.Stage;
import com.apollocurrency.aplwallet.apl.data.ShufflingTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

abstract class ShufflingRepositoryTest {
    ShufflingTestData std;
    ShufflingRepository shufflingRepository;
    @BeforeEach
    void setUp() {
        std = new ShufflingTestData();
        shufflingRepository = repository();
    }

    public abstract ShufflingRepository repository();

    @Test
    void testGetCount() {
        int count = shufflingRepository.getCount();
        assertEquals(7, count);
    }

    @Test
    void testGetActiveCount() {
        int activeCount = shufflingRepository.getActiveCount();
        assertEquals(5, activeCount);
    }

    @Test
    void testExtractAll() {
        List<Shuffling> shufflings = shufflingRepository.extractAll(0, Integer.MAX_VALUE);
        List<Shuffling> expected = List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_3_3_APL_REGISTRATION, std.SHUFFLING_2_2_ASSET_REGISTRATION, std.SHUFFLING_5_1_APL_PROCESSING, std.SHUFFLING_6_1_CURRENCY_REGISTRATION, std.SHUFFLING_7_2_CURRENCY_FINISHED, std.SHUFFLING_4_2_APL_FINISHED);
        assertEquals(expected, shufflings);
        assertEquals(expected, shufflingRepository.extractAll(0, -1));
    }

    @Test
    void testExtractAllWithPagination() {
        List<Shuffling> shufflings = shufflingRepository.extractAll(2, 5);
        List<Shuffling> expected = List.of(std.SHUFFLING_2_2_ASSET_REGISTRATION, std.SHUFFLING_5_1_APL_PROCESSING, std.SHUFFLING_6_1_CURRENCY_REGISTRATION, std.SHUFFLING_7_2_CURRENCY_FINISHED);
        assertEquals(expected, shufflings);
    }


    @Test
    void testGetActiveShufflings() {
        List<Shuffling> activeShufflings = shufflingRepository.getActiveShufflings(0, Integer.MAX_VALUE);
        List<Shuffling> expected = List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_3_3_APL_REGISTRATION,std.SHUFFLING_2_2_ASSET_REGISTRATION, std.SHUFFLING_5_1_APL_PROCESSING, std.SHUFFLING_6_1_CURRENCY_REGISTRATION);

        assertEquals(expected, activeShufflings);
    }

    @Test
    void testGetActiveShufflingWithPagination() {
        List<Shuffling> activeShufflings = shufflingRepository.getActiveShufflings(1, 3);
        List<Shuffling> expected = List.of( std.SHUFFLING_3_3_APL_REGISTRATION, std.SHUFFLING_2_2_ASSET_REGISTRATION, std.SHUFFLING_5_1_APL_PROCESSING);

        assertEquals(expected, activeShufflings);
    }

    @Test
    void testGetActiveShufflingsWithNegativePagination() {
        List<Shuffling> activeShufflings = shufflingRepository.getActiveShufflings(0, -1);
        List<Shuffling> expected = List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_3_3_APL_REGISTRATION,std.SHUFFLING_2_2_ASSET_REGISTRATION, std.SHUFFLING_5_1_APL_PROCESSING, std.SHUFFLING_6_1_CURRENCY_REGISTRATION);

        assertEquals(expected, activeShufflings);
    }

    @Test
    void testGetFinishedShufflings() {
        List<Shuffling> finishedShufflings = shufflingRepository.getFinishedShufflings(0, Integer.MAX_VALUE);
        List<Shuffling> expected = List.of(std.SHUFFLING_7_2_CURRENCY_FINISHED, std.SHUFFLING_4_2_APL_FINISHED);
        assertEquals(expected, finishedShufflings);
        assertEquals(expected, shufflingRepository.getFinishedShufflings(0, -1));
    }

    @Test
    void testGetFinishedShufflingsWithPagination() {
        List<Shuffling> finishedShufflings = shufflingRepository.getFinishedShufflings(1,1);
        List<Shuffling> expected = List.of(std.SHUFFLING_4_2_APL_FINISHED);
        assertEquals(expected, finishedShufflings);
        assertEquals(List.of(), shufflingRepository.getFinishedShufflings(2, Integer.MAX_VALUE));
    }


    @Test
    void testGet() {
        Shuffling shuffling = shufflingRepository.get(std.SHUFFLING_3_3_APL_REGISTRATION.getId());
        assertEquals(std.SHUFFLING_3_3_APL_REGISTRATION, shuffling);
    }

    @Test
    void testGetShufflingWhichNotExist() {
        Shuffling shuffling = shufflingRepository.get(Long.MAX_VALUE);
        assertNull(shuffling);
    }

    @Test
    void testGetHoldingShufflingCount() {
        int count = shufflingRepository.getHoldingShufflingCount(std.SHUFFLING_6_1_CURRENCY_REGISTRATION.getHoldingId(), true);
        assertEquals(3, count);
    }

    @Test
    void testGetHoldingShufflingCountForAplNotFinishedShufflings() {
        int count = shufflingRepository.getHoldingShufflingCount(0, false);
        assertEquals(2, count);
    }

    @Test
    void testGetHoldingShufflingCountForAplFinishedShufflings() {
        int count = shufflingRepository.getHoldingShufflingCount(0, true);
        assertEquals(3, count);
    }

    @Test
    void testGetHoldingShufflingCountForCurrencyWithoutFinished() {
        int count = shufflingRepository.getHoldingShufflingCount(std.SHUFFLING_6_1_CURRENCY_REGISTRATION.getHoldingId(), false);
        assertEquals(2, count);
    }

    @Test
    void testGetCurrencyHoldingShufflings() {
        List<Shuffling> holdingShufflings = shufflingRepository.getHoldingShufflings(std.SHUFFLING_6_1_CURRENCY_REGISTRATION.getHoldingId(), null, true, 0, Integer.MAX_VALUE);
        assertEquals(List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_6_1_CURRENCY_REGISTRATION, std.SHUFFLING_7_2_CURRENCY_FINISHED), holdingShufflings);
    }

    @Test
    void testGetCurrencyHoldingShufflingsWithPagination() {
        List<Shuffling> holdingShufflings = shufflingRepository.getHoldingShufflings(std.SHUFFLING_6_1_CURRENCY_REGISTRATION.getHoldingId(), null, true, 1, 2);
        assertEquals(List.of(std.SHUFFLING_6_1_CURRENCY_REGISTRATION, std.SHUFFLING_7_2_CURRENCY_FINISHED), holdingShufflings);
    }
    @Test
    void testGetCurrencyHoldingShufflingsWithoutFinished() {
        List<Shuffling> holdingShufflings = shufflingRepository.getHoldingShufflings(std.SHUFFLING_6_1_CURRENCY_REGISTRATION.getHoldingId(), null, false, 0, Integer.MAX_VALUE);
        assertEquals(List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_6_1_CURRENCY_REGISTRATION), holdingShufflings);
    }
    @Test
    void testGetAplShufflingsOnRegistrationStage() {
        List<Shuffling> holdingShufflings = shufflingRepository.getHoldingShufflings(0L, Stage.REGISTRATION, true, 0, Integer.MAX_VALUE);
        assertEquals(List.of(std.SHUFFLING_3_3_APL_REGISTRATION), holdingShufflings);
    }


    @Test
    void testGetAssignedShufflings() {
        List<Shuffling> assignedShufflings = shufflingRepository.getAssignedShufflings(1500L, 0, Integer.MAX_VALUE);
        assertEquals(List.of(std.SHUFFLING_5_1_APL_PROCESSING), assignedShufflings);
    }


    @Test
    void testInsert() {
        shufflingRepository.insert(std.NEW_SHUFFLING);
        Shuffling shuffling = shufflingRepository.get(std.NEW_SHUFFLING.getId());
        assertEquals(std.NEW_SHUFFLING, shuffling);
    }

    @Test
    void testInsertExisting() {
        Shuffling existing = std.SHUFFLING_3_3_APL_REGISTRATION.deepCopy();
        existing.setHeight(existing.getHeight() + 1);
        existing.setStage(Stage.PROCESSING);
        shufflingRepository.insert(existing);
        Shuffling shuffling = shufflingRepository.get(existing.getId());
        assertEquals(existing, shuffling);
    }


    @Test
    void testDelete() {
        Shuffling toDelete = std.SHUFFLING_4_2_APL_FINISHED.deepCopy();
        toDelete.setHeight(toDelete.getHeight() + 1);
        shufflingRepository.delete(toDelete);
        Shuffling shuffling = shufflingRepository.get(std.SHUFFLING_4_2_APL_FINISHED.getId());
        assertNull(shuffling);
    }

}
