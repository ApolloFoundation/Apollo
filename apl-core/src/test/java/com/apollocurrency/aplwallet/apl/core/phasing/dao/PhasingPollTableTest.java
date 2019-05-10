/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.EntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
public class PhasingPollTableTest extends EntityDbTableTest<PhasingPoll> {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            PhasingPollServiceImpl.class,
            GlobalSyncImpl.class,
            PhasingPollResultTable.class,
            PhasingPollTable.class,
            PhasingPollVoterTable.class,
            PhasingPollLinkedTransactionTable.class,
            PhasingVoteTable.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();
    @Inject
    Blockchain blockchain;
    @Inject
    PhasingPollTable table;

    PhasingTestData ptd;
    TransactionTestData ttd;

    @Inject
    JdbiHandleFactory jdbiHandleFactory;

    public PhasingPollTableTest() {
        super(PhasingPoll.class);
    }

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @BeforeEach
    @Override
    public void setUp() {
        ptd = new PhasingTestData();
        ttd = new TransactionTestData();
        super.setUp();
    }

    @Override
    public DerivedDbTable<PhasingPoll> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<PhasingPoll> getAll() {
        return List.of(ptd.POLL_0, ptd.POLL_1, ptd.POLL_2, ptd.POLL_3, ptd.POLL_4);
    }


    @Override
    public Blockchain getBlockchain() {
        return blockchain;
    }

    @Override
    public PhasingPoll valueToInsert() {
        return ptd.NEW_POLL;
    }

    @Test
    @Override
    public void testTrim() {
        table.trim(ptd.POLL_1.getFinishHeight() + 1);
        List<PhasingPoll> actual = CollectionUtil.toList(table.getAll(Integer.MIN_VALUE, Integer.MAX_VALUE));
        Assertions.assertEquals(List.of(ptd.POLL_4, ptd.POLL_3), actual);
        String condition = "transaction_id IN " + String.format("(%d,%d,%d)", ptd.POLL_0.getId(), ptd.POLL_1.getId(), ptd.POLL_2.getId());
        assertNotExistEntriesInTableForCondition("phasing_poll_voter", condition);
        assertNotExistEntriesInTableForCondition("phasing_vote", condition);
        assertNotExistEntriesInTableForCondition("phasing_poll_linked_transaction", condition);
    }

    @Test
    public void testTrimAll() {
        table.trim(Integer.MAX_VALUE);
        List<PhasingPoll> actual = CollectionUtil.toList(table.getAll(Integer.MIN_VALUE, Integer.MAX_VALUE));
        Assertions.assertEquals(List.of(), actual);
        String condition = "transaction_id IN " + String.format("(%d,%d,%d,%d,%d)", ptd.POLL_0.getId(), ptd.POLL_1.getId(), ptd.POLL_2.getId(), ptd.POLL_4.getId(), ptd.POLL_3.getId());
        assertNotExistEntriesInTableForCondition("phasing_poll_voter", condition);
        assertNotExistEntriesInTableForCondition("phasing_vote", condition);
        assertNotExistEntriesInTableForCondition("phasing_poll_linked_transaction", condition);
    }

    @Test
    public void testTrimNothing() {
        table.trim(0);
        List<PhasingPoll> actual = CollectionUtil.toList(table.getAll(Integer.MIN_VALUE, Integer.MAX_VALUE));
        Assertions.assertEquals(sortByHeightDesc(getAll()), actual);
    }

    private void assertNotExistEntriesInTableForCondition(String tableName, String condition) {
        DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            try (Statement stmt = con.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM " + tableName + " where  " + condition)) {
                    Assertions.assertFalse(rs.next());
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        });
    }

    @Test
    void testGetFinishingTransactions() {
        List<Transaction> finishingTransactions = table.getFinishingTransactions(ptd.POLL_2.getFinishHeight());

        assertEquals(Arrays.asList(ttd.TRANSACTION_7), finishingTransactions);
    }


    @Test
    void testGetFinishingTransactionsWhenNoTransactionsAtHeight() {
        List<Transaction> finishingTransactions = table.getFinishingTransactions(ttd.TRANSACTION_0.getHeight() - 1);

        assertTrue(finishingTransactions.isEmpty(), "No transactions should be found at height");
    }
    @Test
    void testGetActivePhasingDbIds() throws SQLException {
        List<Long> dbIds = table.getActivePhasedTransactionDbIds(ttd.TRANSACTION_8.getHeight() + 1);
        assertEquals(Arrays.asList(ttd.DB_ID_8, ttd.DB_ID_7), dbIds);
    }

    @Test
    void testGetActivePhasingDbIdWhenHeightIsMax() throws SQLException {
        List<Long> dbIds = table.getActivePhasedTransactionDbIds(Integer.MAX_VALUE);
        assertEquals(Arrays.asList(ttd.DB_ID_12, ttd.DB_ID_11), dbIds);
    }

    @Test
    void testGetActivePhasingDbIdAllPollsFinished() throws SQLException {
        List<Long> dbIds = table.getActivePhasedTransactionDbIds(ptd.POLL_0.getHeight() - 1);
        assertEquals(Collections.emptyList(), dbIds);
    }

    @Test
    void testGetActivePhasingDbIdsWhenNoPollsAtHeight() throws SQLException {
        List<Long> dbIds = table.getActivePhasedTransactionDbIds(ttd.TRANSACTION_0.getHeight());
        assertEquals(Collections.emptyList(), dbIds);
    }

    @Test
    void testGetAllPhasedTransactionsCount() throws SQLException {
        int count = table.getAllPhasedTransactionsCount();

        assertEquals(ptd.NUMBER_OF_PHASED_TRANSACTIONS, count);
    }
    @Test
    void testGetAccountPhasedTransactionsCount() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId());

        assertEquals(2, count);
    }

    @Test
    void testGetNonExistentAccountPhasedTransactionCount() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId() + 1);

        assertEquals(0, count);
    }

    @Test
    void testGetByHoldingId() throws SQLException {
        List<Transaction> transactions = CollectionUtil.toList(table.getHoldingPhasedTransactions(ptd.POLL_5.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ASSET, 0, false, 0, 100));
        assertEquals(List.of(ttd.TRANSACTION_13), transactions);
    }

    @Test
    void testGetByHoldingIdNotExist() throws SQLException {
        List<Transaction> transactions = CollectionUtil.toList(table.getHoldingPhasedTransactions(ptd.POLL_4.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ACCOUNT, 0, false, 0, 100));
        assertTrue(transactions.isEmpty());
    }


}
