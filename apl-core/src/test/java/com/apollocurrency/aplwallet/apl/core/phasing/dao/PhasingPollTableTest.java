/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
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
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
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
import java.util.stream.Collectors;
import javax.inject.Inject;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
public class PhasingPollTableTest extends EntityDbTableTest<PhasingPoll> {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            GlobalSyncImpl.class,
            PhasingPollResultTable.class,
            PhasingPollTable.class,
            PhasingPollVoterTable.class,
            PhasingPollLinkedTransactionTable.class,
            PhasingVoteTable.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class))
            .build();
    @Inject
    Blockchain blockchain;
    @Inject
    PhasingPollTable table;

    PhasingTestData ptd;
    TransactionTestData ttd;

    private BlockTestData btd;

    public PhasingPollTableTest() {
        super(PhasingPoll.class);
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
        return List.of(ptd.POLL_0, ptd.POLL_1, ptd.POLL_2, ptd.POLL_3, ptd.POLL_4, ptd.POLL_5);
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
    public void testTrimForZeroHeight() {
        testTrim(ptd.POLL_1.getFinishHeight() + 1, Integer.MAX_VALUE);
    }

    @Override
    public void testTrim(int height, int blockchainHeight) {
        table.trim(height);
        List<PhasingPoll> actual = CollectionUtil.toList(table.getAll(Integer.MIN_VALUE, Integer.MAX_VALUE));
        List<PhasingPoll> expected = sortByHeightDesc(getAll().stream().filter(p -> p.getFinishHeight() >= height).collect(Collectors.toList()));
        Assertions.assertEquals(expected, actual);
        String condition = "transaction_id IN " + String.format("(%d,%d,%d)", ptd.POLL_0.getId(), ptd.POLL_1.getId(), ptd.POLL_2.getId());
        assertNotExistEntriesInTableForCondition("phasing_poll_voter", condition);
        assertNotExistEntriesInTableForCondition("phasing_vote", condition);
        assertNotExistEntriesInTableForCondition("phasing_poll_linked_transaction", condition);
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
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(ttd.TRANSACTION_8.getHeight() + 1);
        assertEquals(Arrays.asList(new TransactionDbInfo(ttd.DB_ID_8, ttd.TRANSACTION_8.getId()), new TransactionDbInfo(ttd.DB_ID_7, ttd.TRANSACTION_7.getId())), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdWhenHeightIsMax() throws SQLException {
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(Integer.MAX_VALUE);
        assertEquals(Arrays.asList(new TransactionDbInfo(ttd.DB_ID_12, ttd.TRANSACTION_12.getId()), new TransactionDbInfo(ttd.DB_ID_11, ttd.TRANSACTION_11.getId()), new TransactionDbInfo(ttd.DB_ID_13, ttd.TRANSACTION_13.getId())), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdAllPollsFinished() throws SQLException {
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(ptd.POLL_0.getHeight() - 1);
        assertEquals(Collections.emptyList(), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdsWhenNoPollsAtHeight() throws SQLException {
        List<TransactionDbInfo> transactionDbInfoList = table.getActivePhasedTransactionDbIds(ttd.TRANSACTION_0.getHeight());
        assertEquals(Collections.emptyList(), transactionDbInfoList);
    }

    @Test
    void testGetAllPhasedTransactionsCount() throws SQLException {
        int count = table.getAllPhasedTransactionsCount();

        assertEquals(ptd.NUMBER_OF_PHASED_TRANSACTIONS, count);
    }
    @Test
    void testGetAccountPhasedTransactionsCountAtLastBlockHeight() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId(), ptd.POLL_5.getHeight());

        assertEquals(1, count);
    }

    @Test
    void testGetAccountPhasedTransactionsCountAtGenesisBlockHeight() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId(), 0);

        assertEquals(3, count);
    }

    @Test
    void testGetNonExistentAccountPhasedTransactionCount() throws SQLException {
        int count = table.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId() + 1, ptd.POLL_5.getHeight());

        assertEquals(0, count);
    }

    @Test
    void testGetByHoldingId() throws SQLException {
        List<Transaction> transactions = CollectionUtil.toList(table.getHoldingPhasedTransactions(ptd.POLL_5.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ASSET, 0, false, 0, 100, ptd.POLL_5.getHeight()));
        assertEquals(List.of(ttd.TRANSACTION_13), transactions);
    }

    @Test
    void testGetByHoldingIdNotExist() throws SQLException {
        List<Transaction> transactions = CollectionUtil.toList(table.getHoldingPhasedTransactions(ptd.POLL_4.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ACCOUNT, 0, false, 0, 100, ptd.POLL_5.getHeight()));
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testIsTransactionPhasedForPollId() throws SQLException {
        boolean phased = table.isTransactionPhased(ptd.POLL_0.getId());

        assertTrue(phased);
    }

    @Test
    void testIsTransactionPhasedForPollResult() throws SQLException {
        boolean phased = table.isTransactionPhased(ptd.RESULT_0.getId());

        assertTrue(phased);
    }

    @Test
    void testIsTransactionPhasedForOrdinaryTransaction() throws SQLException {
        boolean phased = table.isTransactionPhased(ttd.TRANSACTION_1.getId());

        assertFalse(phased);
    }

    @Test
    void testGetAccountPhasedTransactionsWithPaginationSkipFirstAtLastBlockHeight() throws SQLException {
        List<Transaction> transactions = CollectionUtil.toList(table.getAccountPhasedTransactions(ptd.POLL_0.getAccountId(), 1, 2, ptd.POLL_5.getHeight() - 1));
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testGetAccountPhasedTransactionsWithPaginationSkipFirstAtGenesisBlockHeight() throws SQLException {
        List<Transaction> transactions = CollectionUtil.toList(table.getAccountPhasedTransactions(ptd.POLL_0.getAccountId(), 1, 2, 0));
        assertEquals(List.of(ttd.TRANSACTION_12, ttd.TRANSACTION_11), transactions);
    }

    @Test
    void testGetAllAccountPhasedTransactionsWithPagination() throws SQLException {
        List<Transaction> transactions = CollectionUtil.toList(table.getAccountPhasedTransactions(ptd.POLL_0.getAccountId(), 0, 100, 0));
        assertEquals(List.of(ttd.TRANSACTION_13, ttd.TRANSACTION_12, ttd.TRANSACTION_11), transactions);
    }

    @Test
    void testGetSenderPhasedTransactionFees() throws SQLException {
        long actualFee = table.getSenderPhasedTransactionFees(ptd.POLL_0.getAccountId(), 0);
        long expectedFee = ttd.TRANSACTION_13.getFeeATM() + ttd.TRANSACTION_12.getFeeATM() + ttd.TRANSACTION_11.getFeeATM();
        assertEquals(expectedFee, actualFee);
    }

    @Test
    void testGetSenderPhasedTransactionFeesAtLastPollHeight() throws SQLException {
        long actualFee = table.getSenderPhasedTransactionFees(ptd.POLL_0.getAccountId(), ptd.POLL_5.getHeight());
        long expectedFee = ttd.TRANSACTION_13.getFeeATM();
        assertEquals(expectedFee, actualFee);
    }
    @Test
    void testGetSenderPhasedTransactionFeesForNonExistentAccount() throws SQLException {
        long actualFee = table.getSenderPhasedTransactionFees(1, 0);
        assertEquals(0, actualFee);
    }

    @Test
    void testGetById() {
        PhasingPoll phasingPoll = table.get(ptd.POLL_2.getId());
        assertEquals(ptd.POLL_2, phasingPoll);
    }

    @Test
    void testGetByIdNotExist() {
        PhasingPoll phasingPoll = table.get(1);
        assertNull(phasingPoll);
    }

    @Override
    @Test
    public void testInsertAlreadyExist() {
        PhasingPoll value = ptd.POLL_1;
        Assertions.assertThrows(RuntimeException.class, () -> DbUtils.inTransaction(getDatabaseManager(), (con) -> {
            table.insert(value);
        }));
    }


}
