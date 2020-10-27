/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingApprovedResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollLinkedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollResultTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingPollVoterTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.phasing.PhasingVoteTable;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.impl.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_1;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.TRANSACTION_INDEX_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class ReferencedTransactionDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        TransactionImpl.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
        ReferencedTransactionDaoImpl.class,
        TransactionRowMapper.class,
        TransactionBuilder.class,
        GlobalSyncImpl.class,
        AppendixApplierRegistry.class,
        AppendixValidatorRegistry.class,
        DerivedDbTablesRegistryImpl.class,
        PhasingPollServiceImpl.class, PhasingPollResultTable.class,
        PhasingApprovedResultTable.class,
        PhasingPollLinkedTransactionTable.class, PhasingPollVoterTable.class,
        PhasingVoteTable.class, PhasingPollTable.class,
        FullTextConfigImpl.class,
        BlockDaoImpl.class, TransactionDaoImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
        .addBeans(MockBean.of(mock(AccountService.class), AccountService.class, AccountServiceImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(mock(CurrencyService.class), CurrencyService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .build();

    @Inject
    private ReferencedTransactionDao dao;

    @Test
    void testGetAll() {
        List<Long> allReferencedTransactionIds = dao.getAllReferencedTransactionIds();
        TransactionTestData td = new TransactionTestData();
        Set<Long> expectedIds = td.REFERENCED_TRANSACTIONS.stream().map(ReferencedTransaction::getReferencedTransactionId).collect(Collectors.toSet());
        assertEquals(expectedIds, new HashSet<>(allReferencedTransactionIds));
    }

    @Test
    void testGetById() {
        TransactionTestData td = new TransactionTestData();
        Optional<Long> referencedId = dao.getReferencedTransactionIdFor(td.TRANSACTION_3.getId());
        assertTrue(referencedId.isPresent());
        assertEquals(td.REFERENCED_TRANSACTION_5.getTransactionId(), referencedId.get());
    }

    @Test
    void testGetByIdForTransactionWithoutReferencedTransaction() {
        TransactionTestData td = new TransactionTestData();
        Optional<Long> referencedId = dao.getReferencedTransactionIdFor(td.TRANSACTION_12.getId());

        assertTrue(referencedId.isEmpty());
    }

    @Test
    void testgetByIdForShardTransaction() {
        TransactionTestData td = new TransactionTestData();

        Optional<Long> referencedId = dao.getReferencedTransactionIdFor(TRANSACTION_INDEX_1.getTransactionId());

        assertTrue(referencedId.isPresent());
        assertEquals(td.REFERENCED_TRANSACTION_1.getReferencedTransactionId(), referencedId.get());
    }

    @Test
    void testGetByIdForShardTransactionWithoutReferencedTransactionId() {
        Optional<Long> referencedId = dao.getReferencedTransactionIdFor(TRANSACTION_INDEX_3.getTransactionId());

        assertTrue(referencedId.isEmpty());
    }

    @Test
    void testSave() {
        TransactionTestData td = new TransactionTestData();

        int saveCount = dao.save(td.NOT_SAVED_REFERENCED_SHARD_TRANSACTION);

        assertEquals(1, saveCount);

        Optional<Long> referencedId = dao.getReferencedTransactionIdFor(td.NOT_SAVED_REFERENCED_SHARD_TRANSACTION.getTransactionId());

        assertTrue(referencedId.isPresent());
        assertEquals(td.NOT_SAVED_REFERENCED_SHARD_TRANSACTION.getReferencedTransactionId(), referencedId.get());
    }

    @Test
    void testGetReferencingTransactions() {
        TransactionTestData td = new TransactionTestData();
        List<Transaction> referencingTransactions = dao.getReferencingTransactions(td.TRANSACTION_8.getId(), 0, 100);

        assertEquals(Arrays.asList(td.TRANSACTION_11, td.TRANSACTION_9), referencingTransactions);

    }

    @Test
    void testGetReferencingTransactionsForShardTransaction() {
        TransactionTestData td = new TransactionTestData();
        List<Transaction> referencingTransactions = dao.getReferencingTransactions(td.REFERENCED_TRANSACTION_2.getReferencedTransactionId(), 0, 100);

        assertEquals(Collections.emptyList(), referencingTransactions);
    }

}
