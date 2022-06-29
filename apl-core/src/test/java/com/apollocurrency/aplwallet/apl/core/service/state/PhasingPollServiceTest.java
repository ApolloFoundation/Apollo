/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdaterImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.PhasingTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_0;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
public class PhasingPollServiceTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, DbTestData.getDbUrlProps(), "db/phasing-poll-data.sql", "db/schema.sql");

    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    private TransactionTestData ttd = new TransactionTestData();
    private PublicKeyDao publicKeyDao = mock(PublicKeyDao.class);
    private BlockDao blockDao = mock(BlockDao.class);
    private AccountPublicKeyService publicKeyService = mock(AccountPublicKeyService.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

    BlockTestData blockTestData = new BlockTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        BlockchainImpl.class, DaoConfig.class,
        PhasingPollServiceImpl.class,
        GlobalSyncImpl.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilderFactory.class,
        AppendixApplierRegistry.class,
        AppendixValidatorRegistry.class,
        PhasingPollResultTable.class,
        PhasingPollTable.class,
        PhasingPollVoterTable.class,
        PhasingApprovedResultTable.class,
        PhasingPollLinkedTransactionTable.class,
        PhasingVoteTable.class,
        PublicKeyTable.class,
        FullTextConfigImpl.class,
        AccountGuaranteedBalanceTable.class,
        DerivedDbTablesRegistryImpl.class,
        TransactionDaoImpl.class,
        BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        GenesisPublicKeyTable.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(AccountTable.class), AccountTable.class))
        .addBeans(MockBean.of(mock(AccountService.class), AccountService.class, AccountServiceImpl.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(mock(AliasService.class), AliasService.class))
        .addBeans(MockBean.of(blockDao, BlockDao.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(ttd.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(CurrencyService.class), CurrencyService.class))
        .addBeans(MockBean.of(publicKeyDao, PublicKeyDao.class))
        .addBeans(MockBean.of(publicKeyService, AccountPublicKeyService.class))
        .addBeans(MockBean.of(mock(ReferencedTransactionDao.class), ReferencedTransactionDao.class))
        .addBeans(MockBean.of(mock(FullTextSearchUpdater.class), FullTextSearchUpdater.class, FullTextSearchUpdaterImpl.class))
        .addBeans(MockBean.of(mock(TransactionApplier.class), TransactionApplier.class))
        .build();
    @Inject
    PhasingPollServiceImpl service;
    @Inject
    TransactionService transactionService;
    @Inject
    Blockchain blockchain;

    PhasingTestData ptd;
    BlockTestData btd;

    @Inject
    BlockModelToEntityConverter toEntityConverter;

    @BeforeEach
    void setUp() {
        ptd = new PhasingTestData();
        btd = new BlockTestData();
        doReturn(new PublicKey(1L, new byte[32], 2)).when(publicKeyDao).searchAll(-208393164898941117L);
        doReturn(toEntityConverter.convert(blockTestData.BLOCK_0)).when(blockDao).findLastBlock();
        doReturn(toEntityConverter.convert(blockTestData.BLOCK_3)).when(blockDao).findFirstBlock();
    }

    @Test
    void testGetActivePhasingDbIds() {
        List<TransactionDbInfo> transactionDbInfoList = service.getActivePhasedTransactionDbInfoAtHeight(ttd.TRANSACTION_8.getHeight());
        assertEquals(Arrays.asList(new TransactionDbInfo(ttd.DB_ID_8, ttd.TRANSACTION_8.getId()), new TransactionDbInfo(ttd.DB_ID_7, ttd.TRANSACTION_7.getId())), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdWhenHeightIsMax() {
        extension.cleanAndPopulateDb();

        List<TransactionDbInfo> transactionDbInfoList = service.getActivePhasedTransactionDbInfoAtHeight(ttd.TRANSACTION_12.getHeight());
        assertEquals(Arrays.asList(new TransactionDbInfo(3100, 7), new TransactionDbInfo(ttd.DB_ID_12, ttd.TRANSACTION_12.getId()), new TransactionDbInfo(ttd.DB_ID_11, ttd.TRANSACTION_11.getId())), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdAllNoPollsAtLowHeight() {
        List<TransactionDbInfo> transactionDbInfoList = service.getActivePhasedTransactionDbInfoAtHeight(2998);
        assertEquals(Collections.emptyList(), transactionDbInfoList);
    }

    @Test
    void testGetActivePhasingDbIdsWhenNoPollsAtHighHeight() {
        List<TransactionDbInfo> transactionDbInfoList = service.getActivePhasedTransactionDbInfoAtHeight(553326);
        assertEquals(Collections.emptyList(), transactionDbInfoList);
    }

    @Test
    void testGetPollWithWhitelist() {
        PhasingPoll poll = service.getPoll(ptd.POLL_1.getId());

        assertNotNull(poll);
        assertEquals(ptd.POLL_1, poll);
        assertTrue(poll.fullEquals(ptd.POLL_1));
    }

    @Test
    void testGetPollWhichNotExist() {
        PhasingPoll poll = service.getPoll(ptd.POLL_1.getId() - 1);

        assertNull(poll);
    }

    @Test
    void testGetPollWithoutWhiteList() {
        PhasingPoll poll = service.getPoll(ptd.POLL_3.getId());

        assertNotNull(poll);
        assertEquals(ptd.POLL_3, poll);
        assertTrue(poll.fullEquals(ptd.POLL_3));
    }

    @Test
    void testGetAllPhasedTransactionsCount() {
        extension.cleanAndPopulateDb();

        int count = service.getAllPhasedTransactionsCount();

        assertEquals(ptd.NUMBER_OF_PHASED_TRANSACTIONS, count);
    }

    @Test
    void testGetResult() {
        PhasingPollResult result = service.getResult(ptd.POLL_1.getId());

        assertEquals(ptd.RESULT_2, result);
    }

    @Test
    void testGetResultForNonFinishedPoll() {
        extension.cleanAndPopulateDb();

        PhasingPollResult result = service.getResult(ptd.POLL_3.getId());

        assertNull(result);
    }

    @Test
    void testIsTransactionPhased() {
        boolean phased = service.isTransactionPhased(ttd.TRANSACTION_0.getId());

        assertTrue(phased, "Transaction should be phased");
    }

    @Test
    void testIsTransactionPhasedForShardTransaction() {
        boolean phased = service.isTransactionPhased(TRANSACTION_INDEX_0.getTransactionId());

        assertTrue(phased, "Shard transaction should be phased");
    }

    @Test
    void testTransactionNotPhasedForShardTransaction() {
        boolean phased = service.isTransactionPhased(TRANSACTION_INDEX_1.getTransactionId());

        assertFalse(phased, "Shard transaction should not be phased");
    }

    @Test
    void testTransactionNotPhasedForSimpleTransaction() {
        boolean phased = service.isTransactionPhased(ttd.TRANSACTION_1.getId());

        assertFalse(phased, "Transaction should not be phased");
    }

    @Test
    void testGetFinishingTransactions() {
        List<Transaction> finishingTransactions = service.getFinishingTransactions(ptd.POLL_2.getFinishHeight());

        assertEquals(Arrays.asList(ttd.TRANSACTION_7), finishingTransactions);
    }


    @Test
    void testGetFinishingTransactionsWhenNoTransactionsAtHeight() {
        List<Transaction> finishingTransactions = service.getFinishingTransactions(ttd.TRANSACTION_0.getHeight() - 1);

        assertTrue(finishingTransactions.isEmpty(), "No transactions should be found at height");
    }

    @Test
    void testGetVoterPhasedTransactions() {
        List<Transaction> voterTransactions = service.getVoterPhasedTransactions(ptd.POLL_4_VOTER_0_ID, 0, 100);

        assertEquals(Arrays.asList(ttd.TRANSACTION_11), voterTransactions);
    }

    @Test
    void testGetVoterPhasedTransactionsWnenBlockchainHeightIsHigherThanPollFinishHeight() {
        BlockTestData blockTestData = new BlockTestData();
        blockchain.setLastBlock(blockTestData.LAST_BLOCK);
        List<Transaction> voterTransactions = service.getVoterPhasedTransactions(ptd.POLL_1_VOTER_0_ID, 0, 100);

        assertEquals(0, voterTransactions.size());
    }

    @Test
    void testGetVoterPhasedTransactionForNonExistentVoter() {
        List<Transaction> voterTransactions = service.getVoterPhasedTransactions(ptd.POLL_1_VOTER_0_ID + 1, 0, 100);

        assertEquals(0, voterTransactions.size());
    }

    @Test
    void testGetAccountPhasedTransactionsCount() {
        extension.cleanAndPopulateDb();

        int count = service.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId());

        assertEquals(3, count);
    }

    @Test
    void testGetNonExistentAccountPhasedTransactionCount() {
        int count = service.getAccountPhasedTransactionCount(ttd.TRANSACTION_0.getSenderId() + 1);

        assertEquals(0, count);
    }

    @Test
    void testFinishPollNotApproved() throws SQLException {
        blockchain.setLastBlock(btd.BLOCK_9);
        inTransaction(con -> service.finish(ptd.POLL_3, 1));
        PhasingPollResult result = service.getResult(ptd.POLL_3.getId());
        PhasingPollResult expected = new PhasingPollResult(ptd.RESULT_3.getDbId() + 1, btd.BLOCK_9.getHeight(), ptd.POLL_3.getId(), 1, false);

        assertEquals(expected, result);
    }

    @Test
    void testFinishPollApprovedByLinkedTransactions() throws SQLException {
        extension.cleanAndPopulateDb();

        blockchain.setLastBlock(btd.LAST_BLOCK);
        inTransaction(con -> service.finish(ptd.POLL_3, ptd.POLL_3.getQuorum()));
        PhasingPollResult result = service.getResult(ptd.POLL_3.getId());
        PhasingPollResult expected = new PhasingPollResult(ptd.RESULT_3.getDbId() + 1, btd.LAST_BLOCK.getHeight(), ptd.POLL_3.getId(), ptd.POLL_3.getQuorum(), true);

        assertEquals(expected, result);
    }

    @Test
    void testCountVotesForPollWithLinkedTransactions() {
        extension.cleanAndPopulateDb();

        BlockTestData blockTestData = new BlockTestData();
        blockchain.setLastBlock(blockTestData.LAST_BLOCK);
        long votes = service.countVotes(ptd.POLL_3);

        assertEquals(1, votes);
    }

    @Test
    void testCountVotesForPollWithNewSavedLinkedTransactions() throws SQLException {
        extension.cleanAndPopulateDb();

        BlockTestData blockTestData = new BlockTestData();
        blockchain.setLastBlock(blockTestData.LAST_BLOCK);
        inTransaction(connection -> transactionService.saveTransactions(
            Collections.singletonList(ttd.NOT_SAVED_TRANSACTION))
        );
        long votes = service.countVotes(ptd.POLL_3);

        assertEquals(2, votes);
    }

    @Test
    void testGetVoteCount() {
        extension.cleanAndPopulateDb();

        long votes = service.getVoteCount(ptd.POLL_1.getId());

        assertEquals(2, votes);
    }

    @Test
    void testGetVoteCountForPhasedTransactionWithoutWhiteList() {
        long votes = service.getVoteCount(ptd.POLL_2.getId());

        assertEquals(0, votes);
    }

    @Test
    void testGetVotes() {
        List<PhasingVote> phasingVotes = CollectionUtil.toList(service.getVotes(ptd.POLL_1.getId(), 0, 100));

        assertEquals(Arrays.asList(ptd.POLL_1_VOTE_1, ptd.POLL_1_VOTE_0), phasingVotes);
    }

    @Test
    void testCountVotesForWhitelistedPhasedTransaction() {
        long votes = service.countVotes(ptd.POLL_1);

        assertEquals(2, votes);
    }

    @Test
    void testCountVotesForPhasedTransactionWithoutWhitelist() {
        long votes = service.countVotes(ptd.POLL_2);

        assertEquals(0, votes);
    }

    @Test
    void testGetVote() {
        PhasingVote vote = service.getVote(ptd.POLL_1.getId(), ptd.POLL_1_VOTER_0_ID);

        assertEquals(ptd.POLL_1_VOTE_0, vote);
    }


    @Test
    void testGetVoteForPhasedTransactionWithoutWhitelist() {
        PhasingVote vote = service.getVote(ptd.POLL_2.getId(), ptd.POLL_1_VOTER_0_ID);

        assertNull(vote);
    }

    @Test
    void testAddPoll() throws SQLException {
        blockchain.setLastBlock(btd.BLOCK_10);
        inTransaction(con -> service.addPoll(ttd.TRANSACTION_10, ptd.NEW_POLL_APPENDIX));
        PhasingPoll poll = service.getPoll(ttd.TRANSACTION_10.getId());

        assertEquals(ptd.NEW_POLL, poll);
    }

    @Test
    void testGetApproved() {
        List<PhasingPollResult> phasingPollResults = service.getApproved(ptd.RESULT_1.getHeight());

        assertEquals(Arrays.asList(ptd.RESULT_1), phasingPollResults);
    }

    @Test
    void testGetApprovedForNotApprovedPollResult() {
        List<PhasingPollResult> phasingPollResults = service.getApproved(ptd.RESULT_3.getHeight());

        assertEquals(Collections.emptyList(), phasingPollResults);
    }

    @Test
    void testAddVote() throws SQLException {
        Account account = mock(Account.class);
        doReturn(ptd.NEW_VOTE_TX.getSenderId()).when(account).getId();
        inTransaction(con -> service.addVote(ptd.NEW_VOTE_TX, account, ptd.POLL_1.getId()));
        long voteCount = service.getVoteCount(ptd.POLL_1.getId());

        assertEquals(voteCount, 3);

        PhasingVote vote = service.getVote(ptd.POLL_1.getId(), ptd.NEW_VOTE_TX.getSenderId());

        assertEquals(ptd.NEW_VOTE, vote);
    }

    @Test
    void testGetLinkedFullHashes() {
        List<Transaction> phasedTransactions = service.getLinkedPhasedTransactions(ptd.POLL_3.getLinkedFullHashes().get(0));

        assertEquals(Collections.singletonList(ttd.TRANSACTION_12), phasedTransactions);
    }

    @Test
    void testGetAccountPhasedTransactions() {
        List<Transaction> accountTransactions = service.getAccountPhasedTransactions(ttd.TRANSACTION_9.getSenderId(), 0, 100);
        assertEquals(accountTransactions, List.of(ttd.TRANSACTION_13, ttd.TRANSACTION_12, ttd.TRANSACTION_11));
    }

    @Test
    void testGetAccountPhasedTransactionsForLastBlockWithHeightGreaterThanAllFinishingHeightOfPhasingPolls() {
        Block block = mock(Block.class);
        doReturn(Integer.MAX_VALUE).when(block).getHeight();
        blockchain.setLastBlock(block);
        List<Transaction> accountTransactions = service.getAccountPhasedTransactions(ttd.TRANSACTION_9.getSenderId(), 0, 100);
        assertTrue(accountTransactions.isEmpty());
    }

    void inTransaction(Consumer<Connection> consumer) throws SQLException {
        try (Connection con = extension.getDatabaseManager().getDataSource().begin()) { // start new transaction
            consumer.accept(con);
            extension.getDatabaseManager().getDataSource().commit();
        } catch (SQLException e) {
            extension.getDatabaseManager().getDataSource().rollback();
            throw e;
        }
    }


    @Test
    void testGetByHoldingId() throws SQLException {
        blockchain.setLastBlock(btd.BLOCK_12);
        List<Transaction> transactions = service.getHoldingPhasedTransactions(ptd.POLL_5.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ASSET, 0, false, 0, 100);
        assertEquals(List.of(ttd.TRANSACTION_13), transactions);
    }

    @Test
    void testGetByHoldingIdNotExist() throws SQLException {
        List<Transaction> transactions = service.getHoldingPhasedTransactions(ptd.POLL_4.getVoteWeighting().getHoldingId(), VoteWeighting.VotingModel.ACCOUNT, 0, false, 0, 100);
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testGetSenderPhasedTransactionFees() throws SQLException {
        extension.cleanAndPopulateDb();

        blockchain.setLastBlock(btd.GENESIS_BLOCK);
        long actualFee = service.getSenderPhasedTransactionFees(ptd.POLL_0.getAccountId());
        long expectedFee = ttd.TRANSACTION_13.getFeeATM() + ttd.TRANSACTION_12.getFeeATM() + ttd.TRANSACTION_11.getFeeATM();
        assertEquals(expectedFee, actualFee);
    }

    @Test
    void testGetSenderPhasedTransactionFeesAtLastPollHeight() throws SQLException {
        blockchain.setLastBlock(btd.BLOCK_12);
        long actualFee = service.getSenderPhasedTransactionFees(ptd.POLL_0.getAccountId());
        long expectedFee = ttd.TRANSACTION_13.getFeeATM();
        assertEquals(expectedFee, actualFee);
    }

    @Test
    void testGetSenderPhasedTransactionFeesForNonExistentAccount() throws SQLException {
        blockchain.setLastBlock(btd.GENESIS_BLOCK);
        long actualFee = service.getSenderPhasedTransactionFees(1);
        assertEquals(0, actualFee);
    }

    @Test
    void testVerifyRevealedSecret() {
        boolean verified = PhasingPollService.verifySecret(ptd.POLL_0, "fasfas".getBytes());

        assertTrue(verified);
    }

    @Test
    void testVerifyRevealedSecretForWrongPhasingPoll() {
        boolean verified = PhasingPollService.verifySecret(ptd.POLL_1, "fasfas".getBytes());

        assertFalse(verified);
    }

    @Test
    void testVerifyWrongRevealedSecret() {
        boolean verified = PhasingPollService.verifySecret(ptd.POLL_0, "fasfa".getBytes());

        assertFalse(verified);
    }

}
