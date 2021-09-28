/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.GenesisAccounts;
import com.apollocurrency.aplwallet.apl.core.app.observer.DGSObserver;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountLedgerServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
@Tag("slow")
@EnableWeld
public class DGSObserverTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    Blockchain blockchain = mock(Blockchain.class);
    private TransactionTestData td = new TransactionTestData();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, DaoConfig.class,
        GlobalSyncImpl.class,
        FullTextConfigImpl.class,
        DGSPublicFeedbackTable.class,
        TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionBuilderFactory.class,
        DGSFeedbackTable.class,
        DGSGoodsTable.class,
        DGSTagTable.class,
        DGSPurchaseTable.class,
        DGSServiceImpl.class,
        DGSObserver.class,
        DerivedDbTablesRegistryImpl.class,
        TimeServiceImpl.class, BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class,
        BlockChainInfoServiceImpl.class, AccountServiceImpl.class, GenesisAccounts.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(extension.getFullTextSearchService(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(FullTextSearchUpdater.class), FullTextSearchUpdater.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .addBeans(MockBean.of(mock(AccountGuaranteedBalanceTable.class), AccountGuaranteedBalanceTable.class))
        .addBeans(MockBean.of(mock(ConfigDirProvider.class), ConfigDirProvider.class))
        .addBeans(MockBean.of(mock(AplAppStatus.class), AplAppStatus.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(AccountLedgerService.class), AccountLedgerService.class, AccountLedgerServiceImpl.class))
        .addBeans(MockBean.of(mock(AccountPublicKeyService.class), AccountPublicKeyServiceImpl.class, AccountPublicKeyService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .addBeans(MockBean.of(new AccountTable(extension.getDatabaseManager(), mock(Event.class)), AccountTableInterface.class))
        .build();
    @Inject
    DGSService service;

    @Inject
    AccountService accountService;
    @Inject
    AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;
    @Inject
    Event<Block> event;

    DGSTestData dtd;

    @BeforeEach
    public void setUp() {
        dtd = new DGSTestData();
    }

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }

    @Test
    void testFireEvent() {
        Block lastBlock = mock(Block.class);
        Block prevBlock = mock(Block.class);
        doReturn(dtd.PURCHASE_2.getDeadline()).when(prevBlock).getTimestamp();
        doReturn(dtd.PURCHASE_2.getDeadline() + 60).when(lastBlock).getTimestamp();
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(prevBlock).when(blockchain).getBlock(1L);
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(1_000_000).when(lastBlock).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            event.select(literal(BlockEventType.AFTER_BLOCK_APPLY)).fire(lastBlock);
        });
        verifyAccountBalance(dtd.PURCHASE_2.getBuyerId(), 14725500000000L, 15025000000000L);
        dtd.GOODS_12.setDbId(dtd.GOODS_13.getDbId() + 1);
        dtd.GOODS_12.setHeight(1_000_000);
        dtd.GOODS_12.setQuantity(dtd.GOODS_12.getQuantity() + 1);
        DGSGoods goods = service.getGoods(dtd.GOODS_12.getId());
        assertEquals(dtd.GOODS_12, goods);
        dtd.PURCHASE_2.setDbId(dtd.PURCHASE_18.getDbId() + 1);
        dtd.PURCHASE_2.setHeight(1_000_000);
        dtd.PURCHASE_2.setPending(false);
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_2.getId());
        assertEquals(dtd.PURCHASE_2, purchase);
    }

    @Test
    void testFireEventOnBlockWithZeroHeight() {
        DbUtils.inTransaction(extension, (con) -> {
            event.select(literal(BlockEventType.AFTER_BLOCK_APPLY)).fire(mock(Block.class));
        });
        verifyAccountBalance(dtd.PURCHASE_2.getBuyerId(), 14725000000000L, 15025000000000L);
        DGSGoods goods = service.getGoods(dtd.GOODS_12.getId());
        assertEquals(dtd.GOODS_12, goods);
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_2.getId());
        assertEquals(dtd.PURCHASE_2, purchase);
    }

    private AnnotationLiteral<BlockEvent> literal(BlockEventType blockEventType) {
        return new BlockEventBinding() {
            @Override
            public BlockEventType value() {
                return blockEventType;
            }
        };
    }

    private void verifyAccountBalance(long accountId, Long unconfirmedBalance, Long balance) {
        Account account = accountService.getAccount(accountId);
        if (balance != null) {
            assertEquals(balance, account.getBalanceATM());
        }
        assertEquals(unconfirmedBalance, account.getUnconfirmedBalanceATM());
    }
}
