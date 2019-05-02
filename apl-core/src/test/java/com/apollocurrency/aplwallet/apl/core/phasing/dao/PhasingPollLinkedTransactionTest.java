/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableData;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollLinkedTransaction;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
public class PhasingPollLinkedTransactionTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            GlobalSyncImpl.class,
            PhasingPollLinkedTransactionTable.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .build();
    @Inject
    PhasingPollLinkedTransactionTable table;
    PhasingTestData ptd;
    TransactionTestData ttd;

    @Inject
    JdbiHandleFactory jdbiHandleFactory;

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @BeforeEach
    void setUp() {
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
    }

    @Test
    void testGetAllForPollWithLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(ptd.POLL_3.getId());

        assertEquals(Arrays.asList(ptd.LINKED_TRANSACTION_0, ptd.LINKED_TRANSACTION_1, ptd.LINKED_TRANSACTION_2), linkedTransactions);
    }

    @Test
    void testGetAllForPollWithoutLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(ptd.POLL_1.getId());

        assertTrue(linkedTransactions.isEmpty(), "Linked transactions should not exist for poll2");
    }

    @Test
    void testGetByDbKeyForPollWithLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(new LongKey(ptd.POLL_3.getId()));

        assertEquals(Arrays.asList(ptd.LINKED_TRANSACTION_0, ptd.LINKED_TRANSACTION_1, ptd.LINKED_TRANSACTION_2), linkedTransactions);
    }

    @Test
    void testGetByDbKeyForPollWithoutLinkedTransactions() {
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(new LongKey(ptd.POLL_1.getId()));

        assertTrue(linkedTransactions.isEmpty(), "Linked transactions should not exist for poll2");
    }

    @Test
    void testGetLinkedPhasedTransactions() throws SQLException {
        List<Transaction> transactions = table.getLinkedPhasedTransactions(ptd.LINKED_TRANSACTION_1_HASH);

        assertEquals(List.of(ttd.TRANSACTION_12), transactions);
    }

    @Test
    void testGetLinkedPhasedTransactionsForNonLinkedTransaction() throws SQLException {
        List<Transaction> transactions = table.getLinkedPhasedTransactions(ttd.TRANSACTION_12.getFullHash());

        assertTrue(transactions.isEmpty(), "Linked transactions should not exist for transaction #12");
    }

    @Test
    void testInsertNewLinkedPhasedTransactionWithCaching() throws SQLException {
        List<PhasingPollLinkedTransaction> insertList = List.of(ptd.NEW_LINKED_TRANSACTION_1, ptd.NEW_LINKED_TRANSACTION_2);
        DbUtils.inTransaction(extension, (con) -> {
                    table.insert(insertList);
                    assertInCache(insertList);
                }
        );
        assertNotInCache(insertList);
        DbUtils.inTransaction(extension, (con) -> {
            List<PhasingPollLinkedTransaction> linkedTransactions = table.get(ptd.NEW_LINKED_TRANSACTION_1.getPollId());
            assertEquals(insertList, linkedTransactions);
            assertInCache(insertList);
        });
        List<PhasingPollLinkedTransaction> values = table.getAllByDbId(
                new MinMaxDbId(0, Long.MAX_VALUE), 100).getValues();
        assertEquals(values, List.of(ptd.LINKED_TRANSACTION_0, ptd.LINKED_TRANSACTION_1, ptd.LINKED_TRANSACTION_2, ptd.NEW_LINKED_TRANSACTION_1, ptd.NEW_LINKED_TRANSACTION_2));
    }



    @Test
    void testTruncate() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> table.truncate());
        List<PhasingPollLinkedTransaction> linkedTransactions = table.get(ptd.POLL_3.getId());

        assertTrue(linkedTransactions.isEmpty(), "No phasing linked transactions expected after truncate");
    }


    @Test
    void testDeleteNotSupported() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> table.delete(ptd.LINKED_TRANSACTION_0));
    }

    @Test
    void testInsertWithDifferentKeys() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DbUtils.inTransaction(extension, (con) -> {
            table.insert(List.of(ptd.NEW_LINKED_TRANSACTION_1, ptd.NEW_LINKED_TRANSACTION_3));
        }));
    }

    @Test
    void testRollback() throws SQLException {
        List<PhasingPollLinkedTransaction> insertList1 = List.of(ptd.NEW_LINKED_TRANSACTION_1, ptd.NEW_LINKED_TRANSACTION_2);
        List<PhasingPollLinkedTransaction> insertList2 = List.of(ptd.NEW_LINKED_TRANSACTION_3);
        DbUtils.inTransaction(extension, (con) -> {
                    table.insert(insertList1);
                    table.insert(insertList2);
                    table.rollback(ptd.LINKED_TRANSACTION_0.getHeight() - 1);
                }
        );
        DerivedTableData<PhasingPollLinkedTransaction> derivedTableData = table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100);
        List<PhasingPollLinkedTransaction> values = derivedTableData.getValues();
        assertEquals(List.of(ptd.NEW_LINKED_TRANSACTION_1, ptd.NEW_LINKED_TRANSACTION_2), values);
    }

    @Test
    void testTrimNothing() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> {
                    table.trim(0);
                }
        );
        DerivedTableData<PhasingPollLinkedTransaction> derivedTableData = table.getAllByDbId(new MinMaxDbId(0, Long.MAX_VALUE), 100);
        List<PhasingPollLinkedTransaction> values = derivedTableData.getValues();
        assertEquals(List.of(ptd.LINKED_TRANSACTION_0, ptd.LINKED_TRANSACTION_1, ptd.LINKED_TRANSACTION_2), values);
    }

    @Test
    void testGetAll() {

    }


    void assertInCache(List<PhasingPollLinkedTransaction> txs) {
        List<PhasingPollLinkedTransaction> cachedTxs = getCache(new LongKey(txs.get(0).getPollId()));
        assertEquals(txs, cachedTxs);
    }

    void assertNotInCache(List<PhasingPollLinkedTransaction> txs) {
        List<PhasingPollLinkedTransaction> cachedTxs = getCache(new LongKey(txs.get(0).getPollId()));
        assertNotEquals(txs, cachedTxs);
    }

    List<PhasingPollLinkedTransaction> getCache(DbKey dbKey) {
        if (!extension.getDatabaseManger().getDataSource().isInTransaction()) {
            return DbUtils.getInTransaction(extension, (con) -> getCacheInTransaction(dbKey));
        } else {
            return getCacheInTransaction(dbKey);
        }
    }

    List<PhasingPollLinkedTransaction> getCacheInTransaction(DbKey dbKey) {
        Map<DbKey, Object> cache = extension.getDatabaseManger().getDataSource().getCache(table.getTableName());
        return (List<PhasingPollLinkedTransaction>) cache.get(dbKey);
    }


    void removeFromCache(DbKey dbKey) {
        Map<DbKey, Object> cache = extension.getDatabaseManger().getDataSource().getCache(table.getTableName());
        cache.remove(dbKey);
    }
}
