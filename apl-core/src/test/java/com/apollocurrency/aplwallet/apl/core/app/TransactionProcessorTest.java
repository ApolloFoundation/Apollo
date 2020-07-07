/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_TIMESTAMP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
//    private BlockChainInfoService blockChainInfoService = mock(BlockChainInfoService.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    private PeersService peersService = mock(PeersService.class);
    private TransactionValidator transactionValidator = mock(TransactionValidator.class);
    private TransactionApplier transactionApplier = mock(TransactionApplier.class);
    private UnconfirmedTransactionTable unconfirmedTransactionTable = mock(UnconfirmedTransactionTable.class);
    private DatabaseManager databaseManager = mock(DatabaseManager.class);
    private AccountService accountService = mock(AccountService.class);
    private GlobalSync globalSync = mock(GlobalSync.class);
    private TaskDispatchManager taskDispatchManager = mock(TaskDispatchManager.class);
    private AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);

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
        .addBeans(MockBean.of(unconfirmedTransactionTable, UnconfirmedTransactionTable.class))
        .addBeans(MockBean.of(databaseManager, DatabaseManager.class))
        .addBeans(MockBean.of(accountService, AccountService.class))
        .addBeans(MockBean.of(globalSync, GlobalSync.class))
        .addBeans(MockBean.of(taskDispatchManager, TaskDispatchManager.class))
        .addBeans(MockBean.of(accountPublicKeyService, AccountPublicKeyService.class))
        .build();

    private TransactionProcessor service;
    TransactionTestData td;
//    BlockTestData blockTestData;
    @Mock
    private javax.enterprise.event.Event<List<Transaction>> listEvent;

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();
        service = new TransactionProcessorImpl(propertiesHolder, transactionValidator, transactionApplier,
            listEvent, unconfirmedTransactionTable, databaseManager, accountService,
            globalSync, timeService, ntpTimeConfig.time(), blockchainConfig, taskDispatchManager, peersService, blockchain);
    }

    @Disabled // transaction signature verification FAILES
    void broadcast() throws AplException.ValidationException {
        int expirationTimestamp = timeService.getEpochTime() + 10;
        long senderId = 9211698109297098287L;
        String signature = "75a2e84c1e039205387b025aa8e1e65384f8b455aa3f2a977d65c577caa31f0410a78f6fcaa875a352843c72b7715fd9ec616f8e2e19281b7e247f3d6642c38f";
        Transaction preparedTrx = TransactionTestData.buildTransaction(
            -9128485677221760321L, BLOCK_5_HEIGHT, BLOCK_5_ID, BLOCK_5_TIMESTAMP, (short) 1440, -603599418476309001L,
            (short) 0, 100000000000000000L, 100000000,
            "bfb2f42fa41a5181fc18147b1d9360b4ae06fc65905948fbce127c302201e9a1",
            signature, expirationTimestamp, (byte) 0, (byte) 0, senderId, null,
            null, false, (byte) 1, false, false,
            false, 14734, 2621055931824266697L, false,
            false, false, false, null);
        doReturn(false).when(blockchain).hasTransaction(preparedTrx.getId());
        LongKeyFactory<UnconfirmedTransaction> longKeyFactory = mock(LongKeyFactory.class);
        doReturn(new LongKey(preparedTrx.getId())).when(longKeyFactory).newKey(preparedTrx.getId());
        doReturn(longKeyFactory).when(unconfirmedTransactionTable).getTransactionKeyFactory();
        TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        doReturn(dataSource).when(databaseManager).getDataSource();
        doReturn(signature.getBytes()).when(accountPublicKeyService).getPublicKeyByteArray(senderId);

        service.broadcast(preparedTrx);

        verify(globalSync).writeLock();
        verify(blockchain).hasTransaction(anyLong());
        verify(transactionValidator).validate(any(Transaction.class));
//        verify(service).processTransaction(any(UnconfirmedTransaction.class));
    }

    @Test
    void processPeerTransactions() {
//        service.processPeerTransactions();
    }

}