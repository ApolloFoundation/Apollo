/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.shuffling.mapper.ShufflingMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

@EnableWeld
public class ShufflingTableRepositoryTest extends ShufflingRepositoryTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getInMemDbProps(), "db/shuffling.sql", null);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DerivedTablesRegistry.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class),
                    MockBean.of(mock(Blockchain.class), Blockchain.class))
            .build();
    @Override
    public ShufflingTable repository() {
        ShufflingKeyFactory keyFactory = new ShufflingKeyFactory();
        return new ShufflingTable(keyFactory, new ShufflingMapper(keyFactory));
    }

    @Override
    @Test
    void testDelete() {
        DbUtils.inTransaction(extension, (con)-> super.testDelete());
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
    void testGetAliceShufflings() {
        List<Shuffling> aliceShufflings = repository().getAccountShufflings(std.ALICE_ID, true, 0, Integer.MAX_VALUE);

        assertEquals(List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_3_3_APL_REGISTRATION, std.SHUFFLING_7_2_CURRENCY_FINISHED), aliceShufflings);

    }

    @Test
    void testGetAliceShufflingsWithoutFinished() {
        List<Shuffling> aliceShufflings = repository().getAccountShufflings(std.ALICE_ID, false, 0, Integer.MAX_VALUE);

        assertEquals(List.of(std.SHUFFLING_8_1_CURRENCY_PROCESSING, std.SHUFFLING_3_3_APL_REGISTRATION), aliceShufflings);
    }

    @Test
    void testGetAliceShufflingsWithPagination() {
        List<Shuffling> aliceShufflings = repository().getAccountShufflings(std.ALICE_ID, true, 1, 1);

        assertEquals(List.of(std.SHUFFLING_3_3_APL_REGISTRATION), aliceShufflings);
    }

}
