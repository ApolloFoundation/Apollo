/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.PublicKeyTableProducer;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.DefaultBlockValidator;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ReferencedTransactionService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountControlPhasingServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.BlockChainInfoServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_1;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_2;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_ACCOUNT_ATTACHMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SENDER;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SIGNED_TX_1_HEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled
@EnableWeld
@ExtendWith(MockitoExtension.class)
public class ChildAccountApplyTest {

    public static final int ECBLOCK_HEIGHT = 100_000;
    public static final long ECBLOCK_ID = 121L;
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("txApplierDb").toAbsolutePath().toString()));

    @Mock
    HeightConfig heightConfig;

    @Mock
    TransactionProcessor processor;

    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Blockchain blockchain = mock(Blockchain.class);
    FeeCalculator calculator = mock(FeeCalculator.class);
    NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());


    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(
        GlobalSyncImpl.class, DaoConfig.class,
        AccountTable.class, AccountGuaranteedBalanceTable.class, PublicKeyTableProducer.class,
        AccountServiceImpl.class, BlockChainInfoServiceImpl.class, AccountPublicKeyServiceImpl.class,
        FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class, PropertiesHolder.class,
        DefaultBlockValidator.class, ReferencedTransactionService.class,
        ReferencedTransactionDaoImpl.class,
        TransactionValidator.class, TransactionApplier.class
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
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(AccountControlPhasingService.class), AccountControlPhasingService.class, AccountControlPhasingServiceImpl.class))
        .addBeans(MockBean.of(calculator, FeeCalculator.class))
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
    }

    @Test
    void applyAttachment() throws AplException.NotValidException {
        //GIVEN
        byte[] tx = Convert.parseHexString(SIGNED_TX_1_HEX);
        Transaction.Builder txBuilder = TransactionBuilder.newTransactionBuilder(tx);
        Transaction newTx = txBuilder.build();

        assertNotNull(newTx);

        byte[] txBytes = newTx.getCopyTxBytes();
        byte[] txUnsignedBytes = newTx.getUnsignedBytes();

        String txStr = Convert.toHexString(txBytes);
        String txUnsignedStr = Convert.toHexString(txUnsignedBytes);

        //WHEN
        txApplier.apply(newTx);

        //THEN
        assertEquals(SENDER.getId(), CHILD_1.getParentId());
        assertEquals(SENDER.getId(), CHILD_2.getParentId());

        assertTrue(CHILD_1.isChild());
        assertTrue(CHILD_2.isChild());

        assertTrue(CHILD_1.isMultiSig());
        assertTrue(CHILD_2.isMultiSig());

        assertEquals(CHILD_ACCOUNT_ATTACHMENT.getAddressScope(), CHILD_1.getAddrScope());
        assertEquals(CHILD_ACCOUNT_ATTACHMENT.getAddressScope(), CHILD_2.getAddrScope());

        //Don't remove, sounds weird, but this snippet doesn't work in batch mode.
        /*ArgumentCaptor<Account> argument = ArgumentCaptor.forClass(Account.class);
        verify(accountService, times(2)).update(argument.capture(), eq(false));
        List<Long> args = argument.getAllValues().stream().map(Account::getId).collect(Collectors.toUnmodifiableList());
        assertTrue(args.contains(CHILD_ID_1));
        assertTrue(args.contains(CHILD_ID_2));*/

/*
        verify(accountPublicKeyService).apply(CHILD_1, CHILD_PUBLIC_KEY_1);
        verify(accountPublicKeyService).apply(CHILD_2, CHILD_PUBLIC_KEY_2);
*/
    }

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}