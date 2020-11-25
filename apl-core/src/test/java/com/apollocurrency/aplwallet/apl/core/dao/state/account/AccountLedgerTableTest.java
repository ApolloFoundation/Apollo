/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@QuarkusTest
class AccountLedgerTableTest extends DbContainerBaseTest {

    public static final int TRIM_KEEP = 300;
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");
    @Inject
    AccountLedgerTable table;
    AccountTestData testData = new AccountTestData();
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
/*
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        GlobalSyncImpl.class, AccountLedgerTable.class
    )
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();
*/

    {
        doReturn(TRIM_KEEP).when(propertiesHolder).getIntProperty("apl.ledgerTrimKeep", 30000);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        LedgerEntry previous = table.getEntry(testData.newLedger.getLedgerId(), true);
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newLedger));
        LedgerEntry actual = table.getEntry(testData.newLedger.getLedgerId(), true);

        assertNotNull(actual);
        assertEquals(testData.newLedger.getLedgerId(), actual.getDbId());
        assertEquals(testData.newLedger.getAccountId(), actual.getAccountId());
        assertEquals(testData.newLedger.getEventId(), actual.getEventId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        LedgerEntry previous = table.getEntry(testData.ACC_LEDGER_0.getLedgerId(), true);
        assertNotNull(previous);
        previous.setChange(50000L);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        LedgerEntry actual = table.getEntry(previous.getLedgerId(), true);

        assertNotNull(actual);
        assertEquals(50000L, actual.getChange());
        assertTrue(previous.getDbId() < actual.getDbId());
        assertEquals(previous.getHoldingId(), actual.getHoldingId());
        assertEquals(previous.getTimestamp(), actual.getTimestamp());
    }

    @Test
    void testTrim_on_MAX_height() {
        doReturn(Integer.MAX_VALUE).when(propertiesHolder).BATCH_COMMIT_SIZE();
        doReturn(testData.LEDGER_HEIGHT + TRIM_KEEP).when(blockchain).getHeight();
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(testData.LEDGER_HEIGHT + TRIM_KEEP, true));

        List<LedgerEntry> expected = Collections.emptyList();

        List<LedgerEntry> all = table.getEntries(testData.ACC_LEDGER_4.getAccountId(),
            null, 0, null, 0,
            0, Integer.MAX_VALUE, true);

        assertEquals(expected, all);
    }

    @Test
    void testTrim_on_height() {
        dbExtension.cleanAndPopulateDb();

        doReturn(Integer.MAX_VALUE).when(propertiesHolder).BATCH_COMMIT_SIZE();
        doReturn(testData.LEDGER_HEIGHT - 1 + TRIM_KEEP).when(blockchain).getHeight();
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(testData.LEDGER_HEIGHT, true));

        List<LedgerEntry> expected = testData.ALL_LEDGERS.stream().filter(e -> e.getHeight() > testData.LEDGER_HEIGHT - TRIM_KEEP)
            .sorted(Comparator.comparing(LedgerEntry::getDbId, Comparator.reverseOrder()))
            .collect(Collectors.toList());
        List<LedgerEntry> all = table.getEntries(0, null, 0, null, 0,
            0, Integer.MAX_VALUE, true);

        assertEquals(expected, all);
    }

    @Test
    void getEntry() {
        dbExtension.cleanAndPopulateDb();

        LedgerEntry expected = testData.ACC_LEDGER_0;

        LedgerEntry actual = table.getEntry(testData.ACC_LEDGER_0.getDbId(), true);

        assertEquals(expected, actual);
    }

    @Test
    void getEntries() {
        List<LedgerEntry> expected = List.of(testData.ACC_LEDGER_15, testData.ACC_LEDGER_14, testData.ACC_LEDGER_13,
            testData.ACC_LEDGER_12, testData.ACC_LEDGER_9, testData.ACC_LEDGER_6, testData.ACC_LEDGER_5,
            testData.ACC_LEDGER_2);

        List<LedgerEntry> all = table.getEntries(testData.ACC_LEDGER_9.getAccountId(),
            null, 0, null, 0,
            0, Integer.MAX_VALUE, true);

        assertEquals(expected, all);
    }
}