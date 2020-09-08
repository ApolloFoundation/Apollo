/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTxValidationResult;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_HEIGHT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    private PeersService peersService = mock(PeersService.class);
    private TransactionValidator transactionValidator = mock(TransactionValidator.class);
    private TransactionApplier transactionApplier = mock(TransactionApplier.class);
    private DatabaseManager databaseManager = mock(DatabaseManager.class);
    private AccountService accountService = mock(AccountService.class);
    private GlobalSync globalSync = mock(GlobalSync.class);
    private TaskDispatchManager taskDispatchManager = mock(TaskDispatchManager.class);
    private AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);
    private TransactionBuilder transactionBuilder = mock(TransactionBuilder.class);
    private PrunableLoadingService prunableLoadingService = mock(PrunableLoadingService.class);
    private TransactionTypeFactory transactionTypeFactory = mock(TransactionTypeFactory.class);
    private UnconfirmedTransactionProcessingService processingService = mock(UnconfirmedTransactionProcessingService.class);
    private MemPool memPool = mock(MemPool.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(ntpTimeConfig.time(), NtpTime.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(peersService, PeersService.class))
        .addBeans(MockBean.of(transactionValidator, TransactionValidator.class))
        .addBeans(MockBean.of(transactionApplier, TransactionApplier.class))
        .addBeans(MockBean.of(databaseManager, DatabaseManager.class))
        .addBeans(MockBean.of(accountService, AccountService.class))
        .addBeans(MockBean.of(globalSync, GlobalSync.class))
        .addBeans(MockBean.of(taskDispatchManager, TaskDispatchManager.class))
        .addBeans(MockBean.of(accountPublicKeyService, AccountPublicKeyService.class))
        .addBeans(MockBean.of(transactionTypeFactory, TransactionTypeFactory.class))
        .build();

    private TransactionProcessor service;
    TransactionTestData td;
    @Mock
    private javax.enterprise.event.Event<List<Transaction>> listEvent;

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();
        service = new TransactionProcessorImpl(propertiesHolder, transactionValidator,
            listEvent, databaseManager,
            globalSync, timeService, ntpTimeConfig.time(), blockchainConfig, taskDispatchManager, peersService, blockchain, transactionBuilder, prunableLoadingService, processingService,
            transactionTypeFactory, memPool);
    }

    @Test
    void broadcast() throws AplException.ValidationException {
        //GIVEN
        int expirationTimestamp = timeService.getEpochTime() + 10;
        long senderId = 9211698109297098287L;
        String signature = "75a2e84c1e039205387b025aa8e1e65384f8b455aa3f2a977d65c577caa31f0410a78f6fcaa875a352843c72b7715fd9ec616f8e2e19281b7e247f3d6642c38f";
        Transaction transaction = mock(Transaction.class);
        doReturn(UnconfirmedTxValidationResult.OK_RESULT)
            .when(processingService)
            .validateBeforeProcessing(any(UnconfirmedTransaction.class));
        doReturn(-9128485677221760321L).when(transaction).getId();
        doReturn(100L).when(transaction).getFeeATM();
        doReturn(100).when(transaction).getFullSize();
        doReturn(true).when(transactionValidator).verifySignature(transaction);
        doReturn(false).when(blockchain).hasTransaction(-9128485677221760321L);
        doReturn(BLOCK_5_HEIGHT).when(blockchain).getHeight();
        doReturn(Long.valueOf(BLOCK_5_HEIGHT - 1)).when(blockchainConfig).getLastKnownBlock();
        TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        doReturn(dataSource).when(databaseManager).getDataSource();
        doReturn(signature.getBytes()).when(accountPublicKeyService).getPublicKeyByteArray(senderId);
        doReturn(true).when(transactionApplier).applyUnconfirmed(transaction);
        Event eventType = mock(Event.class);
        doReturn(eventType).when(listEvent).select(any(AnnotationLiteral.class));

        //WHEN
        service.broadcast(transaction);

        //THEN
        verify(globalSync, times(1)).writeLock();
        verify(globalSync, times(1)).writeUnlock();
        verify(blockchain, times(1)).hasTransaction(anyLong());
        verify(transactionValidator).validate(any(Transaction.class));
    }

    @Test
    void processTransaction() throws AplException.ValidationException {
        //GIVEN
        int expirationTimestamp = timeService.getEpochTime() + 10;
        long senderId = 9211698109297098287L;
        String signature = "75a2e84c1e039205387b025aa8e1e65384f8b455aa3f2a977d65c577caa31f0410a78f6fcaa875a352843c72b7715fd9ec616f8e2e19281b7e247f3d6642c38f";
        Transaction transaction = mock(Transaction.class);
        doReturn(UnconfirmedTxValidationResult.OK_RESULT)
            .when(processingService)
            .validateBeforeProcessing(any(UnconfirmedTransaction.class));
        doReturn(100L).when(transaction).getFeeATM();
        doReturn(100).when(transaction).getFullSize();
        UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, expirationTimestamp);
        doReturn(false).when(blockchain).hasTransaction(-9128485677221760321L);
        doReturn(BLOCK_5_HEIGHT).when(blockchain).getHeight();
        doReturn(Long.valueOf(BLOCK_5_HEIGHT - 1)).when(blockchainConfig).getLastKnownBlock();
        TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        doReturn(dataSource).when(databaseManager).getDataSource();
        doReturn(mock(Event.class)).when(listEvent).select(any());
        //WHEN
        service.processPeerTransactions(List.of(unconfirmedTransaction));

        //THEN
        verify(globalSync).writeLock();
        verify(globalSync).writeUnlock();
        verify(blockchain).hasTransaction(anyLong());
    }

}