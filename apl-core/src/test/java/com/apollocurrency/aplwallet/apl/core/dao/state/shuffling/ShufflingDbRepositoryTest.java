/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Tag("slow")
public class ShufflingDbRepositoryTest extends ShufflingRepositoryTest {
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbConfig(), "db/shuffling.sql", null);

    @Override
    public ShufflingTable createRepository() {
        return new ShufflingTable(extension.getDatabaseManager(), mock(Event.class));
    }

    @Override
    @Test
    void testDelete() {
        DbUtils.inTransaction(extension, (con) -> super.testDelete());
    }

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }

    @Override
    @Test
    void testInsert() {
        DbUtils.inTransaction(extension, (con) -> super.testInsert());
    }

    @Override
    @Test
    void testInsertExisting() {
        DbUtils.inTransaction(extension, (con) -> super.testInsertExisting());
    }

    @Test
    void testMerge() {
        DbUtils.inTransaction(extension, (con)-> {
            std.SHUFFLING_3_3_APL_REGISTRATION.setStage(ShufflingStage.BLAME);
            std.SHUFFLING_3_3_APL_REGISTRATION.setHeight(std.SHUFFLING_3_3_APL_REGISTRATION.getHeight()); // set the same height to trigger merge
            int rowsBefore = createRepository().getRowCount();
            createRepository().insert(std.SHUFFLING_3_3_APL_REGISTRATION);

            Shuffling shuffling = createRepository().get(std.SHUFFLING_3_3_APL_REGISTRATION.getId());
            assertEquals(std.SHUFFLING_3_3_APL_REGISTRATION, shuffling);
            int rowAfter = createRepository().getRowCount();
            assertEquals(rowsBefore, rowAfter);
        });
    }


    @Test
    void testGetAliceShufflings() {
        List<Shuffling> aliceShufflings = createRepository().getAccountShufflings(std.ALICE_ID, true, 0, Integer.MAX_VALUE);

        assertEquals(List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_3_3_APL_REGISTRATION, std.SHUFFLING_7_2_CURRENCY_FINISHED), aliceShufflings);

    }

    @Test
    void testGetAliceShufflingsWithoutFinished() {
        List<Shuffling> aliceShufflings = createRepository().getAccountShufflings(std.ALICE_ID, false, 0, Integer.MAX_VALUE);

        assertEquals(List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_3_3_APL_REGISTRATION), aliceShufflings);
    }

    @Test
    void testGetAliceShufflingsWithPagination() {
        List<Shuffling> aliceShufflings = createRepository().getAccountShufflings(std.ALICE_ID, true, 1, 1);

        assertEquals(List.of(std.SHUFFLING_3_3_APL_REGISTRATION), aliceShufflings);
    }

}

