/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing;

import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_0;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.phasing.dao.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingVote;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
public class PhasingPollServiceTest {

    @RegisterExtension
    DbExtension extension = new DbExtension();
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
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();
    @Inject
    PhasingPollService phasingPollService;
    @Inject
    TransactionDao transactionDao;
    @Inject
    Blockchain blockchain;
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
    void testGetActivePhasingDbIds() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ttd.TRANSACTION_8.getHeight() + 1);
        assertEquals(Arrays.asList(ttd.DB_ID_8, ttd.DB_ID_7), dbIds);
    }

    @Test
    void testGetActivePhasingDbIdWhenHeightIsMax() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ttd.TRANSACTION_12.getHeight() + 1);
        assertEquals(Arrays.asList(ttd.DB_ID_12, ttd.DB_ID_11), dbIds);
    }

    @Test
    void testGetActivePhasingDbIdAllPollsFinished() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ptd.POLL_2.getFinishHeight() + 1);
        assertEquals(Collections.emptyList(), dbIds);
    }

    @Test
    void testGetActivePhasingDbIdsWhenNoPollsAtHeight() {
        List<Long> dbIds = phasingPollService.getActivePhasedTransactionDbIdsAtHeight(ttd.TRANSACTION_0.getHeight());
        assertEquals(Collections.emptyList(), dbIds);
    }

    @Test
    void testGetPoll() {
        PhasingPoll poll = phasingPollService.getPoll(ptd.POLL_3.getId());

        assertNotNull(poll);
        assertEquals(ptd.POLL_3, poll);
    }

    @Test
    void testGetPollWhichNotExist() {
        PhasingPoll poll = phasingPollService.getPoll(ptd.POLL_1.getId() - 1);

        assertNull(poll);
    }

    @Test
    void testGetPollWithoutWhiteList() {
        PhasingPoll poll = phasingPollService.getPoll(ptd.POLL_3.getId());

        assertNotNull(poll);
        assertEquals(ptd.POLL_3, poll);
    }

    @Test
    void testGetAllPhasedTransactionsCount() {
        int count = phasingPollService.getAllPhasedTransactionsCount();

        assertEquals(ptd.NUMBER_OF_PHASED_TRANSACTIONS, count);
    }

    @Test
    void testGetResult() {
        PhasingPollResult result = phasingPollService.getResult(ptd.POLL_1.getId());

        assertEquals(ptd.RESULT_1, result);
    }

    @Test
    void testGetResultForNonFinishedPoll() {
        PhasingPollResult result = phasingPollService.getResult(ptd.POLL_3.getId());

        assertNull(result);
    }

    @Test
    void testIsTransactionPhased() {
        boolean phased = phasingPollService.isTransactionPhased(ttd.TRANSACTION_0.getId());

        assertTrue(phased, "Transaction should be phased");
    }

    @Test
    void testIsTransactionPhasedForShardTransaction() {
        boolean phased = phasingPollService.isTransactionPhased(TRANSACTION_INDEX_0.getTransactionId());

        assertTrue(phased, "Shard transaction should be phased");
    }

    @Test
    void testTransactionNotPhasedForShardTransaction() {
        boolean phased = phasingPollService.isTransactionPhased(TRANSACTION_INDEX_1.getTransactionId());

        assertFalse(phased, "Shard transaction should not be phased");
    }

    @Test
    void testTransactionNotPhasedForSimpleTransaction() {
        boolean phased = phasingPollService.isTransactionPhased(ttd.TRANSACTION_1.getId());

        assertFalse(phased, "Transaction should not be phased");
    }

    @Test
    void testGetFinishingTransactions() {
        List<Transaction> finishingTransactions = CollectionUtil.toList(phasingPollService.getFinishingTransactions(ptd.POLL_2.getFinishHeight()));

        assertEquals(Arrays.asList(ttd.TRANSACTION_7), finishingTransactions);
    }


    @Test
    void testGetFinishingTransactionsWhenNoTransactionsAtHeight() {
        List<Transaction> finishingTransactions = CollectionUtil.toList(phasingPollService.getFinishingTransactions(ttd.TRANSACTION_0.getHeight() - 1));

        assertTrue(finishingTransactions.isEmpty(), "No transactions should be found at height");
    }

    @Test
    void testGetVoterPhasedTransactions() {
        List<Transaction> voterTransactions = CollectionUtil.toList(phasingPollService.getVoterPhasedTransactions(ptd.POLL_4_VOTER_0, 0, 100));

        assertEquals(Arrays.asList(ttd.TRANSACTION_11), voterTransactions);
    }

    @Test
    void testGetVoterPhasedTransactionsWnenBlockchainHeightIsHigherThanPollFinishHeight() {
        BlockTestData blockTestData = new BlockTestData();
        blockchain.setLastBlock(blockTestData.BLOCK_11);
        List<Transaction> voterTransactions = CollectionUtil.toList(phasingPollService.getVoterPhasedTransactions(ptd.POLL_1_VOTER_0, 0, 100));

        assertEquals(0, voterTransactions.size());
    }

    @Test
    void testGetVoterPhasedTransactionForNonExistentVoter() {
        List<Transaction> voterTransactions = CollectionUtil.toList(phasingPollService.getVoterPhasedTransactions(ptd.POLL_1_VOTER_0 + 1, 0, 100));

        assertEquals(0, voterTransactions.size());
    }

    @Test
    void testGetAccountPhasedTransactionsCount() {
        int count = phasingPollService.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId());

        assertEquals(2, count);
    }

    @Test
    void testGetNonExistentAccountPhasedTransactionCount() {
        int count = phasingPollService.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId() + 1);

        assertEquals(0, count);
    }

    @Test
    void testFinishPollNotApproved() throws SQLException {
        inTransaction(con -> {
            phasingPollService.finish(ptd.POLL_3, 1);

            PhasingPollResult result = phasingPollService.getResult(ptd.POLL_3.getId());
            PhasingPollResult expected = new PhasingPollResult(ptd.POLL_3.getId(), 1, false, 1);

            assertEquals(expected, result);
        });
    }

    @Test
    void testFinishPollApprovedByLinkedTransactions() throws SQLException {
        inTransaction(con -> {
            phasingPollService.finish(ptd.POLL_3, ptd.POLL_3.getQuorum());
            PhasingPollResult result = phasingPollService.getResult(ptd.POLL_3.getId());
            PhasingPollResult expected = new PhasingPollResult(ptd.POLL_3.getId(), ptd.POLL_3.getQuorum(), true, 1);

            assertEquals(expected, result);
        });
    }

    @Test
    void testCountVotesForPollWithLinkedTransactions() {
        blockchain.setLastBlock(new BlockTestData().BLOCK_11);
        long votes = phasingPollService.countVotes(ptd.POLL_3);

        assertEquals(2, votes);
    }

    @Test
    void testCountVotesForPollWithNewSavedLinkedTransactions() throws SQLException {
        blockchain.setLastBlock(new BlockTestData().BLOCK_11);
        inTransaction(connection -> transactionDao.saveTransactions(connection, Collections.singletonList(ttd.NOT_SAVED_TRANSACTION)));
        long votes = phasingPollService.countVotes(ptd.POLL_3);

        assertEquals(3, votes);
    }

    @Test
    void testGetVoteCount() {
        long votes = phasingPollService.getVoteCount(ptd.POLL_1.getId());

        assertEquals(2, votes);
    }

    @Test
    void testGetVoteCountForPhasedTransactionWithoutWhiteList() {
        long votes = phasingPollService.getVoteCount(ptd.POLL_2.getId());

        assertEquals(0, votes);
    }

    @Test
    void testGetVotes() {
        List<PhasingVote> phasingVotes = CollectionUtil.toList(phasingPollService.getVotes(ptd.POLL_1.getId(), 0, 100));

        assertEquals(Arrays.asList(ptd.POLL_1_VOTE_1, ptd.POLL_1_VOTE_0), phasingVotes);
    }

    @Test
    void testCountVotesForWhitelistedPhasedTransaction() {
        long votes = phasingPollService.countVotes(ptd.POLL_1);

        assertEquals(2, votes);
    }

    @Test
    void testCountVotesForPhasedTransactionWithoutWhitelist() {
        long votes = phasingPollService.countVotes(ptd.POLL_2);

        assertEquals(0, votes);
    }

    @Test
    void testGetVote() {
        PhasingVote vote = phasingPollService.getVote(ptd.POLL_1.getId(), ptd.POLL_1_VOTER_0);

        assertEquals(ptd.POLL_1_VOTE_0, vote);
    }


    @Test
    void testGetVoteForPhasedTransactionWithoutWhitelist() {
        PhasingVote vote = phasingPollService.getVote(ptd.POLL_2.getId(), ptd.POLL_1_VOTER_0);

        assertNull(vote);
    }

    @Test
    void testAddPoll() throws SQLException {
        inTransaction(con -> {
                    phasingPollService.addPoll(ttd.TRANSACTION_10, ptd.NEW_POLL_APPENDIX);

                    PhasingPoll poll = phasingPollService.getPoll(ttd.TRANSACTION_10.getId());

                    assertEquals(ptd.NEW_POLL, poll);
                }
        );
    }

    @Test
    void testGetApproved() {
        List<PhasingPollResult> phasingPollResults = CollectionUtil.toList(phasingPollService.getApproved(ptd.RESULT_1.getHeight()));

        assertEquals(Arrays.asList(ptd.RESULT_1), phasingPollResults);
    }

    @Test
    void testGetApprovedForNotApprovedPollResult() {
        List<PhasingPollResult> phasingPollResults = CollectionUtil.toList(phasingPollService.getApproved(ptd.RESULT_2.getHeight()));

        assertEquals(Collections.emptyList(), phasingPollResults);
    }

    @Test
    void testAddVote() throws SQLException {
        inTransaction(con -> {
                    phasingPollService.addVote(ptd.NEW_VOTE_TX, new Account(ptd.NEW_VOTE_TX.getSenderId()), ptd.POLL_1.getId());
                    long voteCount = phasingPollService.getVoteCount(ptd.POLL_1.getId());

                    assertEquals(voteCount, 3);

                    PhasingVote vote = phasingPollService.getVote(ptd.POLL_1.getId(), ptd.NEW_VOTE_TX.getSenderId());

                    assertEquals(ptd.NEW_VOTE, vote);
                }
        );
    }

    @Test
    void testGetLinkedFullHashes() {
        List<Transaction> phasedTransactions = phasingPollService.getLinkedPhasedTransactions(ptd.POLL_3.getLinkedFullHashes().get(0));

        assertEquals(Collections.singletonList(ttd.TRANSACTION_12), phasedTransactions);
    }


    void inTransaction(Consumer<Connection> consumer) throws SQLException {
        try (Connection con = extension.getDatabaseManger().getDataSource().begin()) { // start new transaction
            consumer.accept(con);
            extension.getDatabaseManger().getDataSource().commit();
        }
        catch (SQLException e) {
            extension.getDatabaseManger().getDataSource().rollback();
            throw e;
        }
    }
}
