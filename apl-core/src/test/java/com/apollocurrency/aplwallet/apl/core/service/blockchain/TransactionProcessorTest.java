/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private TimeService timeService = mock(TimeService.class);
    private PeersService peersService = mock(PeersService.class);
    private TransactionValidator transactionValidator = mock(TransactionValidator.class);
    private TransactionApplier transactionApplier = mock(TransactionApplier.class);
    private DatabaseManager databaseManager = mock(DatabaseManager.class);
    private AccountService accountService = mock(AccountService.class);
    private GlobalSync globalSync = mock(GlobalSync.class);
    private TaskDispatchManager taskDispatchManager = mock(TaskDispatchManager.class);
    private AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);
    private TransactionBuilderFactory transactionBuilderFactory = mock(TransactionBuilderFactory.class);
    private PrunableLoadingService prunableLoadingService = mock(PrunableLoadingService.class);
    private TransactionTypeFactory transactionTypeFactory = mock(TransactionTypeFactory.class);
    private UnconfirmedTransactionProcessingService processingService = mock(UnconfirmedTransactionProcessingService.class);
    private UnconfirmedTransactionCreator unconfirmedTransactionCreator = mock(UnconfirmedTransactionCreator.class);
    private MemPool memPool = mock(MemPool.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
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
        .addBeans(MockBean.of(unconfirmedTransactionCreator, UnconfirmedTransactionCreator.class))
        .build();

    private TransactionProcessor processor;
    TransactionTestData td;
    @Mock
    private javax.enterprise.event.Event<List<Transaction>> listEvent;

    @BeforeAll
    static void beforeAll() {
        Convert2.init("APL", 0);
    }

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();
        processor = new TransactionProcessorImpl(transactionValidator,
            listEvent, databaseManager,
            globalSync, timeService, blockchainConfig, peersService, blockchain, transactionBuilderFactory, prunableLoadingService,
            processingService,
            unconfirmedTransactionCreator,
            memPool);
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
        UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, expirationTimestamp, 10, 176);
        doReturn(unconfirmedTransaction).when(unconfirmedTransactionCreator).from(eq(transaction), anyLong());
        doReturn(true).when(transactionValidator).verifySignature(transaction);
//        doReturn(true).when(processingService).addNewUnconfirmedTransaction(any(UnconfirmedTransaction.class));
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
        processor.broadcast(transaction);

        //THEN
        verify(blockchain, times(1)).hasTransaction(anyLong());
        verify(transactionValidator).validateFully(any(Transaction.class));
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
//        doReturn(true).when(processingService).addNewUnconfirmedTransaction(any(UnconfirmedTransaction.class));
        UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, expirationTimestamp, 10, 176);
        doReturn(unconfirmedTransaction).when(unconfirmedTransactionCreator).from(eq(transaction), anyLong());
        doReturn(BLOCK_5_HEIGHT).when(blockchain).getHeight();
        doReturn(Long.valueOf(BLOCK_5_HEIGHT - 1)).when(blockchainConfig).getLastKnownBlock();
        TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        doReturn(dataSource).when(databaseManager).getDataSource();
        doReturn(mock(Event.class)).when(listEvent).select(any());
        doReturn(mock(TransactionalDataSource.StartedConnection.class)).when(dataSource).beginTransactionIfNotStarted();
        doReturn(true).when(memPool).canAccept(1);
        //WHEN
        processor.processPeerTransactions(List.of(transaction));

        //THEN
        verify(transactionValidator).validateSufficiently(transaction);
        verify(memPool).addPendingProcessing(unconfirmedTransaction);
    }


    @Test
    void processLater() {
        List<Transaction> txsToProcess = List.of(td.TRANSACTION_0, td.TRANSACTION_1);
        when(timeService.systemTimeMillis()).thenReturn(1000L);
        when(blockchain.hasTransaction(td.TRANSACTION_0.getId())).thenReturn(true);
        UnconfirmedTransaction createdUnconfirmedTx = mock(UnconfirmedTransaction.class);
        when(unconfirmedTransactionCreator.from(td.TRANSACTION_1, 1000)).thenReturn(createdUnconfirmedTx);
        td.TRANSACTION_1.setBlock(mock(Block.class));
        td.TRANSACTION_1.fail("Test error message");

        processor.processLater(txsToProcess);

        verify(memPool).processLater(createdUnconfirmedTx);
        assertNull(td.TRANSACTION_1.getBlock(), "Block should be nullified for the tx, processed later");
        assertTrue(td.TRANSACTION_1.getErrorMessage().isEmpty(), "Tx error message should be reseted after processLater operation");
        verifyNoMoreInteractions(memPool);
    }

}