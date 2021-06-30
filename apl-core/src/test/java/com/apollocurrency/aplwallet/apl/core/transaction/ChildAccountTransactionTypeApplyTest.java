/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.GenesisAccounts;
import com.apollocurrency.aplwallet.apl.core.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
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
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.types.child.CreateChildTransactionType;
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
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_1;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_2;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_ACCOUNT_ATTACHMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_PUBLIC_KEY_1;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_PUBLIC_KEY_2;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SENDER;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SIGNED_TX_1_HEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("slow")
@EnableWeld
@ExtendWith(MockitoExtension.class)
class ChildAccountTransactionTypeApplyTest extends DbContainerBaseTest {

    public static final int ECBLOCK_HEIGHT = 100_000;
    public static final long ECBLOCK_ID = 121L;

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    PublicKeyDao publicKeyDao = mock(PublicKeyDao.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

    Blockchain blockchain = mock(Blockchain.class);
    FeeCalculator calculator = mock(FeeCalculator.class);
    AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);
    NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();


    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(
        GlobalSyncImpl.class, DaoConfig.class, AccountGuaranteedBalanceTable.class, PublicKeyTableProducer.class,
        AccountServiceImpl.class, GenesisAccounts.class, BlockChainInfoServiceImpl.class, AccountPublicKeyServiceImpl.class,
        FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class, PropertiesHolder.class,
        DefaultBlockValidator.class, ReferencedTransactionService.class,
        AppendixApplierRegistry.class,
        AppendixValidatorRegistry.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionEntityToModelConverter.class,
        ReferencedTransactionDaoImpl.class,
        TransactionBuilderFactory.class,
        TransactionValidator.class, TransactionApplier.class, JdbiHandleFactory.class, JdbiConfiguration.class
    )
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(InMemoryCacheManager.class), InMemoryCacheManager.class))
        .addBeans(MockBean.of(mock(TaskDispatchManager.class), TaskDispatchManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class, PhasingPollServiceImpl.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(mock(GeneratorService.class), GeneratorService.class))
        .addBeans(MockBean.of(publicKeyDao, PublicKeyDao.class))
        .addBeans(MockBean.of(mock(TransactionVersionValidator.class), TransactionVersionValidator.class))
        .addBeans(MockBean.of(mock(BlockSerializer.class), BlockSerializer.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(AccountControlPhasingService.class), AccountControlPhasingService.class, AccountControlPhasingServiceImpl.class))
        .addBeans(MockBean.of(calculator, FeeCalculator.class))
        .addBeans(MockBean.of(new AccountTable(extension.getDatabaseManager(), mock(Event.class)), AccountTableInterface.class))
        .build();

    @Inject
    TransactionApplier txApplier;

    @Inject
    AccountService accountService;


    @BeforeEach
    void setUp() {
        CHILD_1.setParentId(0L);
        CHILD_1.setMultiSig(false);
        CHILD_2.setParentId(0L);
        CHILD_2.setMultiSig(false);

        EcBlockData ecBlockData = new EcBlockData(ECBLOCK_ID, ECBLOCK_HEIGHT);
        when(blockchain.getECBlock(300)).thenReturn(ecBlockData);
        Convert2.init("APL", 1739068987193023818L);
    }

    @Test
    void applyAttachment() throws AplException.NotValidException {
        //GIVEN
        CreateChildTransactionType type = new CreateChildTransactionType(blockchainConfig, accountService, accountPublicKeyService, blockchain);
        TransactionBuilderFactory builder = new TransactionBuilderFactory(new CachedTransactionTypeFactory(List.of(type)), blockchainConfig);
        byte[] tx = Convert.parseHexString(SIGNED_TX_1_HEX);
        Transaction newTx = builder.newTransaction(tx);

        long senderId = AccountService.getId(newTx.getSenderPublicKey());
        when(publicKeyDao.searchAll(senderId)).thenReturn(new PublicKey(senderId, newTx.getSenderPublicKey(), newTx.getHeight()));

        assertNotNull(newTx);

        //WHEN
        DbUtils.inTransaction(extension, connection -> txApplier.apply(newTx));

        //THEN
        verify(accountPublicKeyService).apply(any(Account.class), eq(CHILD_PUBLIC_KEY_1));
        verify(accountPublicKeyService).apply(any(Account.class), eq(CHILD_PUBLIC_KEY_2));

        Account child1 = accountService.getAccount(CHILD_1.getId());
        Account child2 = accountService.getAccount(CHILD_2.getId());

        assertEquals(SENDER.getId(), child1.getParentId());
        assertEquals(SENDER.getId(), child2.getParentId());

        assertTrue(child1.isChild());
        assertTrue(child2.isChild());

        assertTrue(child1.isMultiSig());
        assertTrue(child2.isMultiSig());

        assertEquals(CHILD_ACCOUNT_ATTACHMENT.getAddressScope(), child1.getAddrScope());
        assertEquals(CHILD_ACCOUNT_ATTACHMENT.getAddressScope(), child2.getAddrScope());
    }
}