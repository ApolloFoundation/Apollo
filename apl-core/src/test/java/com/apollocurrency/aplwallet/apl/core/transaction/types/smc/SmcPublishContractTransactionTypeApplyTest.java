/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.smc.ContractModelToStateEntityConverter;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractStateTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.smc.SmcContractTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.model.smc.AplAddress;
import com.apollocurrency.aplwallet.apl.core.rest.service.ServerInfoService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockSerializer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountControlPhasingServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.BlockChainInfoServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.ContractServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.internal.AplBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistryHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendixApplier;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.smc.contract.SmartContract;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
//@Tag("slow")
@EnableWeld
@ExtendWith(MockitoExtension.class)
class SmcPublishContractTransactionTypeApplyTest extends DbContainerBaseTest {

    public static final int ECBLOCK_HEIGHT = 100_000;
    public static final long ECBLOCK_ID = 121L;

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    ServerInfoService serverInfoService = mock(ServerInfoService.class);
    PublicKeyDao publicKeyDao = mock(PublicKeyDao.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    Blockchain blockchain = mock(Blockchain.class);
    FeeCalculator calculator = mock(FeeCalculator.class);
    AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);
    NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(
        GlobalSyncImpl.class, DaoConfig.class,
        AccountTable.class, AccountGuaranteedBalanceTable.class, PublicKeyTableProducer.class,
        AccountServiceImpl.class, BlockChainInfoServiceImpl.class, AccountPublicKeyServiceImpl.class,
        FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class, PropertiesHolder.class,
        DefaultBlockValidator.class, ReferencedTransactionService.class,
        PublicKeyAnnouncementAppendixApplier.class, AppendixApplierRegistry.class,
        AppendixValidatorRegistry.class,
        //TransactionRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        ReferencedTransactionDaoImpl.class,
        TransactionValidator.class, TransactionApplier.class,
        SmcConfig.class, AplBlockchainIntegratorFactory.class,
        SmcContractTable.class, SmcContractStateTable.class, ContractModelToEntityConverter.class, ContractModelToStateEntityConverter.class,
        ContractServiceImpl.class
    )
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
        .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class, PhasingPollServiceImpl.class))
        .addBeans(MockBean.of(serverInfoService, ServerInfoService.class))
        .addBeans(MockBean.of(mock(TransactionEntityToModelConverter.class), TransactionEntityToModelConverter.class))
        .addBeans(MockBean.of(mock(TransactionEntityRowMapper.class), TransactionEntityRowMapper.class))
        //.addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(mock(GeneratorService.class), GeneratorService.class))
        .addBeans(MockBean.of(publicKeyDao, PublicKeyDao.class))
        .addBeans(MockBean.of(mock(TransactionVersionValidator.class), TransactionVersionValidator.class))
        .addBeans(MockBean.of(mock(BlockSerializer.class), BlockSerializer.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(AccountControlPhasingService.class), AccountControlPhasingService.class, AccountControlPhasingServiceImpl.class))
        .addBeans(MockBean.of(calculator, FeeCalculator.class))
        .build();

    @Inject
    TransactionApplier txApplier;
    @Inject
    AccountService accountService;
    @Inject
    ContractService contractService;
    @Inject
    AplBlockchainIntegratorFactory integratorFactory;
    @Inject
    PublicKeyAnnouncementAppendixApplier applier;
    @Inject
    AppendixApplierRegistry registry;
    AccountService spyAccountService;

    TxBContext context;
    TransactionTypeFactory transactionTypeFactory;
    TransactionBuilderFactory transactionBuilderFactory;

    @BeforeAll
    static void beforeAll() {
        Convert2.init("APL", 1739068987193023818L);
    }

    @BeforeEach
    void setUp() {
        AppendixApplierRegistryHelper.addApplier(applier, registry);
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(100).when(blockchain).getHeight();
        spyAccountService = spy(accountService);
        context = TxBContext.newInstance(chain);
        transactionTypeFactory = new CachedTransactionTypeFactory(List.of(
            new SmcPublishContractTransactionType(blockchainConfig, spyAccountService, contractService, integratorFactory)
        ));
        transactionBuilderFactory = new TransactionBuilderFactory(transactionTypeFactory, blockchainConfig);
    }

    @Test
    void applyAttachment() throws AplException.NotValidException {
        //GIVEN
        //Account account = new Account(3705364957971254799L, 1000000000L, 1000000000L, 1000000000L, 0L, 10);
        byte[] announcedPublicKey = Convert.parseHexString("30B7C63FDE11877D7AFFD4C64F47F314DC1D7BAAA7FAF4FAB905A8BD61E6A732".toLowerCase(Locale.ROOT));

        doNothing().when(spyAccountService).addToBalanceATM(any(Account.class), any(LedgerEvent.class), eq(-7715091681280036635L), eq(-10L), eq(-500000L));
        doNothing().when(spyAccountService).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), eq(-7715091681280036635L), eq(10L));

        //Rlp encoded Tx V3
        String expectedTxBytes = "0b30a466666666663662642d303061332d333436622d616164362d3631666566633062643163368205a084060246ca82014b887acb9b4da22ff07001a039dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d3796015288461282f08a3184740a6482138880f861f83b8001844465616c8d636c617373204465616c207b7d9a226669727374506172616d222c3132332c2230783938373635228a6a617661736372697074e30401a030b7c63fde11877d7affd4c64f47f314dc1d7baaa7faf4fab905a8bd61e6a732c0f84df84b8839dc2e813bb45ff0b84055719abd90fb66d63da4d2fafc88b7f270edb7ffe399fd569b3d4478cbd3300fe6092c96b309de1adbf9a80fa04e67d1fa5dd68e60901fd81640c772e87f520e";

        //WHEN
        Transaction newTx = transactionBuilderFactory.newTransaction(Convert.parseHexString(expectedTxBytes));

        //THEN
        assertEquals(3, newTx.getVersion());
        long senderId = AccountService.getId(newTx.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId)).thenReturn(new PublicKey(senderId, newTx.getSenderPublicKey(), newTx.getHeight()));

        assertNotNull(newTx);

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx));

        SmartContract smartContract = contractService.loadContract(new AplAddress(newTx.getRecipientId()));
        //THEN
        assertNotNull(smartContract);
        assertEquals(new AplAddress(newTx.getId()).getHex(), smartContract.getTxId().getHex());
    }
}