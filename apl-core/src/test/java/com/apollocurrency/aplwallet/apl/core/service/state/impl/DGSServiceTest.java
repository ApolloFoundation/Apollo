/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSTag;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountLedgerServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsDelivery;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsListing;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DigitalGoodsPurchase;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.data.DGSTestData.BUYER_0_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.BUYER_1_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.BUYER_2_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.GOODS_0_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.GOODS_1_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.GOODS_2_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.GOODS_3_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.SELLER_0_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.SELLER_1_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
@Tag("slow")
@EnableWeld
public class DGSServiceTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    Blockchain blockchain = mock(Blockchain.class);
    AccountTable accountTable = new AccountTable(extension.getDatabaseManager(), mock(Event.class));
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class,
        TimeServiceImpl.class,
        GlobalSyncImpl.class,
        FullTextConfigImpl.class,
        DGSPublicFeedbackTable.class,
        DGSFeedbackTable.class,
        DGSGoodsTable.class,
        DGSTagTable.class,
        DGSPurchaseTable.class,
        DGSServiceImpl.class,
        DerivedDbTablesRegistryImpl.class,
        BlockChainInfoServiceImpl.class, AccountServiceImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .addBeans(MockBean.of(mock(ConfigDirProvider.class), ConfigDirProvider.class))
        .addBeans(MockBean.of(mock(AplAppStatus.class), AplAppStatus.class))
        .addBeans(MockBean.of(mock(FullTextSearchService.class), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(FullTextSearchEngine.class), FullTextSearchEngine.class))
        .addBeans(MockBean.of(mock(AccountGuaranteedBalanceTable.class), AccountGuaranteedBalanceTable.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .addBeans(MockBean.of(accountTable, AccountTableInterface.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(AccountPublicKeyService.class), AccountPublicKeyServiceImpl.class, AccountPublicKeyService.class))
        .addBeans(MockBean.of(mock(AccountLedgerService.class), AccountLedgerService.class, AccountLedgerServiceImpl.class))
        .addBeans(MockBean.of(mock(FullTextSearchUpdater.class), FullTextSearchUpdater.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .build();
    Block lastBlock = mock(Block.class);
    Block prevBlock = mock(Block.class);
    @Inject
    DGSService service;
    @Inject
    DGSGoodsTable goodsTable;

    @Inject
    AccountService accountService;
    @Inject
    AccountGuaranteedBalanceTable accountGuaranteedBalanceTable;

    DGSTestData dtd;


    @BeforeEach
    public void setUp() {
        dtd = new DGSTestData();
    }

    @AfterEach
    void tearDown() throws IOException {
        extension.cleanAndPopulateDb();
    }

    @Test
    void testGetPurchaseCount() {
        int purchaseCount = service.getPurchaseCount();
        assertEquals(6, purchaseCount);
    }

    @Test
    void testGetPurchaseCountWithFeedbacks() {
        int purchaseCount = service.getPurchaseCount(true, false);

        assertEquals(3, purchaseCount);
    }

    @Test
    void testGetPurchaseCountForCompletedPurchases() {
        int purchaseCount = service.getPurchaseCount(false, true);

        assertEquals(4, purchaseCount);
    }

    @Test
    void testGetPurchaseCountForCompletedPurchasesWithPublicFeedback() {
        int purchaseCount = service.getPurchaseCount(true, true);

        assertEquals(3, purchaseCount);
    }

    @Test
    void testGetDefaultPurchaseCount() {
        int purchaseCount = service.getPurchaseCount(false, false);

        assertEquals(6, purchaseCount);
        int defaultPurchaseCount = service.getPurchaseCount();
        assertEquals(defaultPurchaseCount, purchaseCount);
    }

    @Test
    void testGetAllPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_8, dtd.PURCHASE_2, dtd.PURCHASE_18), dgsPurchases);
    }

    @Test
    void testGetAllPurchasesWithOffset() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(2, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8, dtd.PURCHASE_2, dtd.PURCHASE_18), dgsPurchases);
    }

    @Test
    void testGetAllPurchasesWithLimit() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(0, 3));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
    }

    @Test
    void testGetAllPurchasesWithLimitAndOffset() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(2, 3));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
    }

    @Test
    void testGetByDefaultAllPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getPurchases(false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_8, dtd.PURCHASE_2, dtd.PURCHASE_18), dgsPurchases);
    }

    @Test
    void testGetAllPurchasesWithPublicFeedback() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getPurchases(true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5), dgsPurchases);
    }

    @Test
    void testGetAllCompletedPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getPurchases(false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
    }

    @Test
    void testGetAllCompletedPurchasesWithPublicFeedbacks() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getPurchases(true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5), dgsPurchases);
    }

    @Test
    void testGetAllCompletedPurchasesWithPublicFeedbackWithPagination() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getPurchases(false, false, 1, 2));
        assertEquals(List.of(dtd.PURCHASE_14, dtd.PURCHASE_5), dgsPurchases);
    }

    @Test
    void testGetSellerPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_0_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_2, dtd.PURCHASE_18), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_1_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
    }

    @Test
    void testGetPurchasesForSellerWhichNotExist() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(1L, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), dgsPurchases);
    }

    @Test
    void testGetSellerPurchasesWithFeedbacks() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(dgsPurchases, List.of(dtd.PURCHASE_16, dtd.PURCHASE_14));
    }

    @Test
    void testGetSellerCompletedPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_0_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_1_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
    }

    @Test
    void testGetSellerPurchasesWithPagination() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_0_ID, true, true, 1, 1));
        assertEquals(dgsPurchases, List.of(dtd.PURCHASE_14));
    }

    @Test
    void testGetSellerPurchaseCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, false, false);
        assertEquals(4, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, false, false);
        assertEquals(2, sellerPurchaseCount);
    }

    @Test
    void testGetSellerPurchaseWithFeedbacksCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, true, false);
        assertEquals(2, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, true, false);
        assertEquals(1, sellerPurchaseCount);
    }

    @Test
    void testGetSellerCompletedPurchaseCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, false, true);
        assertEquals(2, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, false, true);
        assertEquals(2, sellerPurchaseCount);
    }

    @Test
    void testGetSellerCompletedPurchaseWithFeedbacksCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, true, true);
        assertEquals(2, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, true, true);
        assertEquals(1, sellerPurchaseCount);
    }

    @Test
    void testGetBuyerPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_0_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_2_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14, dtd.PURCHASE_2), dgsPurchases);
    }

    @Test
    void testGetBuyerPurchasesForDeletedBuyerPurchase() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_1_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), dgsPurchases);
    }

    @Test
    void testGetBuyerPurchasesWithFeedback() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_2_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), dgsPurchases);
    }

    @Test
    void testGetBuyerCompletedPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_0_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_2_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), dgsPurchases);
    }

    @Test
    void testGetBuyerCompletedPurchasesWithFeedback() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_0_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_2_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), dgsPurchases);
    }

    @Test
    void testGetBuyerPurchasesWithPagination() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_0_ID, false, false, 1, 2));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getBuyerPurchases(BUYER_2_ID, false, false, 1, 1));
        assertEquals(List.of(dtd.PURCHASE_2), dgsPurchases);
    }

    @Test
    void testGetBuyerPurchaseCountWithFeedback() {
        int buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_0_ID, true, false);
        assertEquals(2, buyerPurchaseCount);
        buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_2_ID, true, false);
        assertEquals(1, buyerPurchaseCount);
    }

    @Test
    void testGetBuyerCompletedPurchaseCount() {
        int buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_0_ID, false, true);
        assertEquals(3, buyerPurchaseCount);
        buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_2_ID, false, true);
        assertEquals(1, buyerPurchaseCount);
    }

    @Test
    void testGetBuyerCompletedPurchaseCountWithFeedback() {
        int buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_0_ID, true, true);
        assertEquals(2, buyerPurchaseCount);
        buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_2_ID, true, true);
        assertEquals(1, buyerPurchaseCount);
    }

    @Test
    void testGetBuyerPurchaseCount() {
        int buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_0_ID, false, false);
        assertEquals(3, buyerPurchaseCount);
        buyerPurchaseCount = service.getBuyerPurchaseCount(BUYER_2_ID, false, false);
        assertEquals(2, buyerPurchaseCount);
    }

    @Test
    void testGetSellerBuyerPurchases() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14, dtd.PURCHASE_2), purchases);
    }

    @Test
    void testGetSellerBuyerPurchasesForSellerAndBuyerWithoutMutualPurchases() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_1_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
    }

    @Test
    void testGetSellerBuyerCompletedPurchases() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
    }

    @Test
    void testGetSellerBuyerPurchasesWithFeedback() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
    }

    @Test
    void testGetSellerBuyerCompletedPurchasesWithFeedback() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
    }

    @Test
    void testGetSellerBuyerPurchasesWithPagination() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, false, false, 0, 1));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, false, false, 1, 1));
        assertEquals(List.of(dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, false, false, 1, 1));
        assertEquals(List.of(dtd.PURCHASE_2), purchases);
    }


    @Test
    void testGetSellerBuyerPurchaseCount() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_0_ID, false, false);
        assertEquals(1, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, false, false);
        assertEquals(2, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_2_ID, false, false);
        assertEquals(2, purchaseCount);
    }

    @Test
    void testGetSellerBuyerPurchaseCountForSellerAndBuyerWithoutMutualPurchases() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_1_ID, false, false);

        assertEquals(0, purchaseCount);
    }

    @Test
    void testGetSellerBuyerCompletedPurchaseCount() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_0_ID, false, true);
        assertEquals(1, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, false, true);
        assertEquals(2, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_2_ID, false, true);
        assertEquals(1, purchaseCount);
    }

    @Test
    void testGetSellerBuyerPurchaseWithFeedbackCount() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_0_ID, true, false);
        assertEquals(1, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, true, false);
        assertEquals(1, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_2_ID, true, false);
        assertEquals(1, purchaseCount);
    }

    @Test
    void testGetSellerBuyerCompletedPurchaseWithFeedbackCount() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_0_ID, true, true);
        assertEquals(1, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, true, true);
        assertEquals(1, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_2_ID, true, true);
        assertEquals(1, purchaseCount);
    }

    @Test
    void testGetGoodsPurchases() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 0, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, BUYER_0_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 1, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_1_ID, 0, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_2_ID, 0, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, BUYER_2_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_2), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, 0, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_2), purchases);
    }

    @Test
    void testGetGoodsPurchasesWithFeedback() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 0, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, BUYER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 1, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_1_ID, 0, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_2_ID, 0, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, BUYER_2_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, 0, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
    }

    @Test
    void testGetGoodsCompletedPurchases() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 0, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, BUYER_0_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 1, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_1_ID, 0, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_2_ID, 0, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, BUYER_2_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, 0, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
    }

    @Test
    void testGetGoodsCompletedPurchasesWithFeedback() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 0, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, BUYER_0_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_1_ID, 0, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_2_ID, 0, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
    }

    @Test
    void testGetGoodsPurchasesWithPagination() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, 0, false, false, 1, 1));
        assertEquals(List.of(dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_0_ID, BUYER_0_ID, false, false, 0, 1));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_1_ID, 0, false, false, 1, 10));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_2_ID, 0, false, false, 0, 0));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, BUYER_2_ID, false, false, 1, 3));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getGoodsPurchases(GOODS_3_ID, 0, false, false, 0, 2));
        assertEquals(List.of(dtd.PURCHASE_2), purchases);
    }

    @Test
    void testGetGoodsPurchaseCount() {
        int goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_0_ID, false, false);
        assertEquals(2, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_1_ID, false, false);
        assertEquals(1, goodsPurchaseCount);
    }

    @Test
    void testGetGoodsPurchaseCountForUnknownGoods() {
        int goodsPurchaseCount = service.getGoodsPurchaseCount(1, false, false);
        assertEquals(0, goodsPurchaseCount);
    }

    @Test
    void testGetGoodsPurchaseCountWithFeedback() {
        int goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_0_ID, true, false);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_1_ID, true, false);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_2_ID, true, false);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_3_ID, true, false);
        assertEquals(0, goodsPurchaseCount);
    }

    @Test
    void testGetGoodsCompletedPurchaseCount() {
        int goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_0_ID, false, true);
        assertEquals(2, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_1_ID, false, true);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_2_ID, false, true);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_3_ID, false, true);
        assertEquals(0, goodsPurchaseCount);
    }

    @Test
    void testGetGoodsCompletedPurchaseCountWithFeedback() {
        int goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_0_ID, true, true);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_1_ID, true, true);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_2_ID, true, true);
        assertEquals(1, goodsPurchaseCount);
        goodsPurchaseCount = service.getGoodsPurchaseCount(GOODS_3_ID, true, true);
        assertEquals(0, goodsPurchaseCount);
    }

    @Test
    void testGetPurchaseById() {
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_16.getId());

        assertEquals(dtd.PURCHASE_16, purchase);
    }

    @Test
    void testGetNotLatestPurchaseById() {
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_17.getId());
        assertNull(purchase, "Deleted purchase should not be retrieved from db");
    }

    @Test
    void testGetUnknownPurchaseByDbId() {
        DGSPurchase purchase = service.getPurchase(1);
        assertNull(purchase, "Purchase with id = 1 should not exist");
    }

    @Test
    void testGetPendingSellerPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getPendingSellerPurchases(SELLER_0_ID, 0, 2));
        assertEquals(List.of(dtd.PURCHASE_2), dgsPurchases);
    }

    @Test
    void testGetPendingSellerPurchasesForSellerWithoutPendingPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getPendingSellerPurchases(SELLER_1_ID, 0, 1));
        assertEquals(List.of(), dgsPurchases);
    }

    @Test
    void testGetExpiredSellerPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getExpiredSellerPurchases(SELLER_0_ID, 0, 1));

        assertEquals(List.of(dtd.PURCHASE_18), dgsPurchases);
    }

    @Test
    void testGetExpiredSellerPurchasesForSellerWithoutExpiredPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getExpiredSellerPurchases(SELLER_1_ID, 0, 1));

        assertEquals(List.of(), dgsPurchases);
    }

    @Test
    void testGetPendingPurchase() {
        DGSPurchase pendingPurchase = service.getPendingPurchase(dtd.PURCHASE_2.getId());

        assertEquals(dtd.PURCHASE_2, pendingPurchase);
    }

    @Test
    void testGetPendingPurchaseForNotPendingPurchase() {
        DGSPurchase pendingPurchase = service.getPendingPurchase(dtd.PURCHASE_8.getId());

        assertNull(pendingPurchase, "Purchase should not be pending");
    }

    @Test
    void testGetPendingPurchaseForNotLatestPurchase() {
        DGSPurchase pendingPurchase = service.getPendingPurchase(dtd.PURCHASE_6.getId());

        assertNull(pendingPurchase, "Not latest purchase should not be pending");
    }

    @Test
    void testGetExpiredPendingPurchasesByBlock() {
        doReturn(dtd.PURCHASE_2.getDeadline()).when(prevBlock).getTimestamp();
        doReturn(dtd.PURCHASE_2.getDeadline() + 60).when(lastBlock).getTimestamp();
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(prevBlock).when(blockchain).getBlock(1L);

        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getExpiredPendingPurchases(lastBlock));

        assertEquals(List.of(dtd.PURCHASE_2), dgsPurchases);
    }


    @Test
    void testGetExpiredPendingPurchasesByBlockBelowPurchaseDeadline() {
        doReturn(dtd.PURCHASE_2.getDeadline() - 60).when(prevBlock).getTimestamp();
        doReturn(dtd.PURCHASE_2.getDeadline()).when(lastBlock).getTimestamp();
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(prevBlock).when(blockchain).getBlock(1L);

        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getExpiredPendingPurchases(lastBlock));

        assertEquals(List.of(), dgsPurchases);
    }

    @Test
    void testGetExpiredPendingPurchasesByBlockAbovePurcaseDeadline() {
        doReturn(dtd.PURCHASE_2.getDeadline() + 1).when(prevBlock).getTimestamp();
        doReturn(dtd.PURCHASE_2.getDeadline() + 61).when(lastBlock).getTimestamp();
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(prevBlock).when(blockchain).getBlock(1L);

        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getExpiredPendingPurchases(lastBlock));

        assertEquals(List.of(), dgsPurchases);
    }

    @Test
    void testSetPendingForPurchase() {
        dtd.PURCHASE_8.setHeight(dtd.PURCHASE_8.getHeight() + 10_000);
        DbUtils.inTransaction(extension, (con) -> service.setPending(dtd.PURCHASE_8, true));
        DGSPurchase pendingPurchase = service.getPendingPurchase(dtd.PURCHASE_8.getId());
        dtd.PURCHASE_8.setDbId(dtd.PURCHASE_18.getDbId() + 1);
        assertEquals(dtd.PURCHASE_8, pendingPurchase);
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(0, Integer.MAX_VALUE));
        assertTrue(dgsPurchases.contains(dtd.PURCHASE_8));
        assertEquals(6, dgsPurchases.size());
    }

    @Test
    void testSetPendingForNewPurchase() {
        DbUtils.inTransaction(extension, (con) -> {
            service.setPending(dtd.NEW_PURCHASE, false);
        });
        DGSPurchase pendingPurchase = service.getPurchase(dtd.NEW_PURCHASE.getId());
        assertEquals(dtd.NEW_PURCHASE, pendingPurchase);
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(0, Integer.MAX_VALUE));
        assertTrue(dgsPurchases.contains(dtd.NEW_PURCHASE));
        assertEquals(7, dgsPurchases.size());
    }

    @Test
    void testGetFeedbacks() {
        dtd.PURCHASE_5.setFeedbacks(null);
        List<DGSFeedback> feedbacks = service.getFeedbacks(dtd.PURCHASE_5);
        assertEquals(dtd.PURCHASE_0_FEEDBACKS, feedbacks);
        assertEquals(dtd.PURCHASE_0_FEEDBACKS, dtd.PURCHASE_5.getFeedbacks());
    }

    @Test
    void testGetFeedbackForPurchaseWithoutFeedbacks() {
        List<DGSFeedback> feedbacks = service.getFeedbacks(dtd.PURCHASE_2);
        assertNull(feedbacks);
        assertNull(dtd.PURCHASE_2.getFeedbacks());
    }

    @Test
    void testGetPublicFeedbacks() {
        dtd.PURCHASE_14.setPublicFeedbacks(null);
        List<DGSPublicFeedback> publicFeedbacks = service.getPublicFeedbacks(dtd.PURCHASE_14);
        assertEquals(dtd.PURCHASE_6_PUBLIC_FEEDBACKS, publicFeedbacks);
        assertEquals(dtd.PURCHASE_6_PUBLIC_FEEDBACKS, dtd.PURCHASE_14.getPublicFeedbacks());
    }

    @Test
    void testGetPublicFeedbacksForPurchaseWithoutPublicFeedbacks() {
        dtd.PURCHASE_2.setPublicFeedbacks(null);
        List<DGSPublicFeedback> publicFeedbacks = service.getPublicFeedbacks(dtd.PURCHASE_2);
        assertNull(publicFeedbacks);
        assertNull(dtd.PURCHASE_2.getFeedbacks());
    }

    @Test
    void testAddPublicFeedback() {
        doReturn(dtd.PURCHASE_18.getHeight() + 1000).when(blockchain).getHeight();
        DGSTestData tdCopy = new DGSTestData();
        List<DGSPublicFeedback> expected = new ArrayList<>(tdCopy.PURCHASE_16.getPublicFeedbacks());

        expected.get(0).setHeight(blockchain.getHeight());
        expected.get(1).setHeight(blockchain.getHeight());
        DbUtils.inTransaction(extension, (con) -> {

            dtd.PURCHASE_16.setHeight(dtd.PURCHASE_18.getHeight() + 1000);
            dtd.PURCHASE_16.setDbId(dtd.PURCHASE_18.getDbId());

            DGSPublicFeedback publicFeedback = new DGSPublicFeedback(0L, blockchain.getHeight(), "New public feedback added", dtd.PURCHASE_16.getId());
            expected.add(publicFeedback);

            service.feedback(dtd.PURCHASE_16.getId(), null, new MessageAppendix(publicFeedback.getFeedback()));
        });
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_16.getId());
        assertEquals(tdCopy.PURCHASE_16.getHeight(), purchase.getHeight());
        long initialId = tdCopy.PUBLIC_FEEDBACK_13.getDbId();
        for (DGSPublicFeedback dgsFeedback : expected) {
            dgsFeedback.setDbId(++initialId);
        }
        List<DGSPublicFeedback> feedbacks = service.getPublicFeedbacks(purchase);
        assertEquals(expected, feedbacks);

    }

    @Test
    void testAddPublicFeedbackForPurchaseWithoutPublicFeedbacks() {
        doReturn(dtd.PURCHASE_18.getHeight() + 1000).when(blockchain).getHeight();
        DGSPublicFeedback expected = new DGSPublicFeedback(0L, blockchain.getHeight(), "New public feedback added", dtd.PURCHASE_2.getId());
        dtd.PURCHASE_2.setHeight(dtd.PURCHASE_18.getHeight() + 1000);
        dtd.PURCHASE_2.setDbId(dtd.PURCHASE_18.getDbId());
        DbUtils.inTransaction(extension, (con) -> {
            service.feedback(dtd.PURCHASE_2.getId(), null, new MessageAppendix(expected.getFeedback()));
        });
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_2.getId());
        assertEquals(blockchain.getHeight(), purchase.getHeight());
        assertTrue(purchase.hasPublicFeedbacks());

        expected.setDbId(dtd.PUBLIC_FEEDBACK_13.getDbId() + 1);
        List<DGSPublicFeedback> feedbacks = service.getPublicFeedbacks(purchase);
        assertIterableEquals(List.of(expected), feedbacks);
    }

    @Test
    void testAddFeedback() {
        doReturn(dtd.PURCHASE_18.getHeight() + 2000).when(blockchain).getHeight();
        DGSTestData tdCopy = new DGSTestData();
        List<DGSFeedback> expected = new ArrayList<>(tdCopy.PURCHASE_5.getFeedbacks());
        expected.forEach(e -> e.setHeight(blockchain.getHeight()));
        DbUtils.inTransaction(extension, (con) -> {

            dtd.PURCHASE_14.setHeight(dtd.PURCHASE_18.getHeight() + 2000);

            DGSFeedback feedback = new DGSFeedback(0L, blockchain.getHeight(), dtd.PURCHASE_5.getId(), new EncryptedData(new byte[32], new byte[32]));
            expected.add(feedback);

            service.feedback(dtd.PURCHASE_5.getId(), new EncryptedMessageAppendix(feedback.getFeedbackEncryptedData(), false, true), null);
        });
        long initialId = tdCopy.FEEDBACK_11.getDbId();
        for (DGSFeedback dgsFeedback : expected) {
            dgsFeedback.setDbId(++initialId);
        }
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_5.getId());
        assertEquals(tdCopy.PURCHASE_5.getHeight(), purchase.getHeight());
        List<DGSFeedback> feedbacks = service.getFeedbacks(purchase);
        assertEquals(expected, feedbacks);
    }

    @Test
    void testAddFeedbackForPurchaseWithoutPublicFeedbacks() {
        doReturn(dtd.PURCHASE_18.getHeight() + 1000).when(blockchain).getHeight();
        List<DGSFeedback> expected = new ArrayList<>();
        DbUtils.inTransaction(extension, (con) -> {

            dtd.PURCHASE_2.setHeight(dtd.PURCHASE_18.getHeight() + 1000);
            dtd.PURCHASE_2.setDbId(dtd.PURCHASE_18.getDbId());

            DGSFeedback feedback = new DGSFeedback(0L, blockchain.getHeight(), dtd.PURCHASE_2.getId(), new EncryptedData("New feedback added".getBytes(), new byte[32]));
            expected.add(feedback);

            service.feedback(dtd.PURCHASE_2.getId(), new EncryptedMessageAppendix(feedback.getFeedbackEncryptedData(), true, false), null);
        });
        expected.get(0).setDbId(dtd.FEEDBACK_11.getDbId() + 1);
        DGSPurchase savedPurchase = service.getPurchase(dtd.PURCHASE_2.getId());
        List<DGSFeedback> feedbacks = service.getFeedbacks(savedPurchase);
        assertEquals(expected, feedbacks);
        assertEquals(blockchain.getHeight(), savedPurchase.getHeight());
        assertTrue(savedPurchase.hasFeedbacks());
    }

    @Test
    void testAddPublicFeedbackWithEncryptedFeedback() {
        int blockchainHeight = dtd.PURCHASE_18.getHeight() + 4000;
        doReturn(blockchainHeight).when(blockchain).getHeight();

        DGSTestData tdCopy = new DGSTestData();
        List<DGSFeedback> expectedFeedbacks = new ArrayList<>(tdCopy.PURCHASE_5.getFeedbacks());
        expectedFeedbacks.forEach(e -> e.setHeight(blockchainHeight));
        DGSFeedback expectedFeedback = new DGSFeedback(0L, blockchainHeight, dtd.PURCHASE_5.getId(), new EncryptedData(new byte[32], new byte[32]));
        expectedFeedbacks.add(expectedFeedback);
        List<DGSPublicFeedback> expectedPublicFeedbacks = new ArrayList<>(tdCopy.PURCHASE_5.getPublicFeedbacks());
        expectedPublicFeedbacks.forEach(e -> e.setHeight(blockchainHeight));
        DGSPublicFeedback expectedPublicFeedback = new DGSPublicFeedback(0L, blockchainHeight, "New public feedback", tdCopy.PURCHASE_5.getId());
        expectedPublicFeedbacks.add(expectedPublicFeedback);
        long initialEncryptedFeedbackId = tdCopy.FEEDBACK_11.getDbId();
        for (DGSFeedback dgsFeedback : expectedFeedbacks) {
            dgsFeedback.setDbId(++initialEncryptedFeedbackId);
        }
        long initialPublicFeedbackId = tdCopy.PUBLIC_FEEDBACK_13.getDbId();
        for (DGSPublicFeedback publicFeedback : expectedPublicFeedbacks) {
            publicFeedback.setDbId(++initialPublicFeedbackId);
        }
        DbUtils.inTransaction(extension, (con) -> {
            service.feedback(dtd.PURCHASE_5.getId(), new EncryptedMessageAppendix(expectedFeedback.getFeedbackEncryptedData(), false, true), new MessageAppendix(expectedPublicFeedback.getFeedback()));
        });


        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_5.getId());
        assertTrue(purchase.hasPublicFeedbacks());
        assertTrue(purchase.hasFeedbacks());
        assertEquals(tdCopy.PURCHASE_5.getHeight(), purchase.getHeight());
        List<DGSFeedback> feedbacks = service.getFeedbacks(purchase);
        assertEquals(expectedFeedbacks, feedbacks);
        List<DGSPublicFeedback> publicFeedbacks = service.getPublicFeedbacks(purchase);
        assertEquals(expectedPublicFeedbacks, publicFeedbacks);
    }

    @Test
    void testAddPublicFeedbackWithEncryptedFeedbackToPurchaseWithoutFeedbacks() {
        int blockchainHeight = dtd.PURCHASE_18.getHeight() + 4000;
        doReturn(blockchainHeight).when(blockchain).getHeight();

        DGSTestData tdCopy = new DGSTestData();
        DGSFeedback expectedFeedback = new DGSFeedback(dtd.FEEDBACK_11.getDbId() + 1, blockchainHeight, dtd.PURCHASE_2.getId(), new EncryptedData("New encrypted feedback".getBytes(), new byte[32]));
        DGSPublicFeedback expectedPublicFeedback = new DGSPublicFeedback(dtd.PUBLIC_FEEDBACK_13.getDbId() + 1, blockchainHeight, "New public feedback", tdCopy.PURCHASE_2.getId());
        DbUtils.inTransaction(extension, (con) -> service.feedback(dtd.PURCHASE_2.getId(), new EncryptedMessageAppendix(expectedFeedback.getFeedbackEncryptedData(), false, true), new MessageAppendix(expectedPublicFeedback.getFeedback())));


        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_2.getId());
        assertTrue(purchase.hasPublicFeedbacks());
        assertTrue(purchase.hasFeedbacks());
        assertEquals(blockchainHeight, purchase.getHeight());
        List<DGSFeedback> feedbacks = service.getFeedbacks(purchase);
        assertEquals(List.of(expectedFeedback), feedbacks);
        List<DGSPublicFeedback> publicFeedbacks = service.getPublicFeedbacks(purchase);
        assertEquals(List.of(expectedPublicFeedback), publicFeedbacks);
    }

    @Test
    void testListGoods() {
        Transaction listTransaction = mock(Transaction.class);
        int height = 100_000;
        long txId = 100L;
        long senderId = 200L;
        String tag1 = dtd.TAG_5.getTag();
        String tag2 = dtd.TAG_10.getTag();
        String tag3 = "newtag";
        String tag4 = "batman";
        doReturn(height).when(blockchain).getHeight();
        PrunablePlainMessageAppendix image = new PrunablePlainMessageAppendix("Image");
        doReturn(100_000).when(blockchain).getLastBlockTimestamp();
        doReturn(image).when(listTransaction).getPrunablePlainMessage();
        doReturn(height).when(listTransaction).getHeight();
        doReturn(txId).when(listTransaction).getId();
        doReturn(senderId).when(listTransaction).getSenderId();
        String tags = String.join(",", List.of(tag1, tag2, tag3, tag4));
        DGSGoods expected = new DGSGoods(dtd.GOODS_13.getDbId() + 1, height, txId, senderId, "Test goods", "Test", tags, new String[]{tag1, tag2, tag3}, 100_000, true, 2, 100_000_000, false);
        DigitalGoodsListing digitalGoodsListing = new DigitalGoodsListing("Test goods", "Test", tags, 2, 100_000_000);
        DbUtils.inTransaction(extension, (con) -> {
            service.listGoods(listTransaction, digitalGoodsListing);
        });
        DGSGoods actual = service.getGoods(txId);
        assertEquals(expected, actual);
        List<DGSTag> dgsTags = CollectionUtil.toList(service.getAllTags(0, Integer.MAX_VALUE));
        DGSTag expectedTag1 = new DGSTag(dtd.TAG_12.getDbId() + 1, height, tag1, dtd.TAG_5.getInStockCount() + 1, dtd.TAG_5.getTotalCount() + 1);
        DGSTag expectedTag2 = new DGSTag(dtd.TAG_12.getDbId() + 2, height, tag2, dtd.TAG_10.getInStockCount() + 1, dtd.TAG_10.getTotalCount() + 1);
        DGSTag expectedTag3 = new DGSTag(dtd.TAG_12.getDbId() + 3, height, tag3, 1, 1);
        assertEquals(7, dgsTags.size());
        assertEquals(7, service.getTagsCount());
        assertTrue(dgsTags.contains(expectedTag1));
        assertTrue(dgsTags.contains(expectedTag2));
        assertTrue(dgsTags.contains(expectedTag3));

    }

    @Test
    void testGetTagsCount() {
        assertEquals(6, service.getTagsCount());
    }

    @Test
    void testGetCountInStock() {
        assertEquals(4, service.getCountInStock());
    }

    @Test
    void testGetAllTags() {
        List<DGSTag> tags = CollectionUtil.toList(service.getAllTags(0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.TAG_10, dtd.TAG_4, dtd.TAG_11, dtd.TAG_12, dtd.TAG_6, dtd.TAG_5), tags);
    }

    @Test
    void testGetAllTagsWithPagination() {
        List<DGSTag> tags = CollectionUtil.toList(service.getAllTags(1, 4));
        assertEquals(List.of(dtd.TAG_4, dtd.TAG_11, dtd.TAG_12, dtd.TAG_6), tags);
    }

    @Test
    void testGetAllInStockTags() {
        List<DGSTag> tags = CollectionUtil.toList(service.getInStockTags(0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.TAG_10, dtd.TAG_4, dtd.TAG_11, dtd.TAG_12), tags);
    }

    @Test
    void testGetAllInStockTagsWithPagination() {
        List<DGSTag> tags = CollectionUtil.toList(service.getInStockTags(1, 2));
        assertEquals(List.of(dtd.TAG_4, dtd.TAG_11), tags);
    }

    @Test
    void testGetTagsLike() {
        List<DGSTag> tags = CollectionUtil.toList(service.getTagsLike("s", false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.TAG_5, dtd.TAG_10), tags);
    }

    @Test
    void testGetTagsLikeInStock() {
        List<DGSTag> tags = CollectionUtil.toList(service.getTagsLike("s", true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.TAG_10), tags);
    }

    @Test
    void testGetTagsLinkeWithPagination() {
        List<DGSTag> tags = CollectionUtil.toList(service.getTagsLike("s", false, 0, 0));
        assertEquals(List.of(dtd.TAG_5), tags);
    }

    @Test
    void testDelistGoods() {
        doReturn(100_000).when(blockchain).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            service.delistGoods(dtd.GOODS_12.getId());
        });
        DGSGoods goods = service.getGoods(dtd.GOODS_12.getId());
        dtd.GOODS_12.setHeight(100_000);
        dtd.GOODS_12.setDbId(dtd.GOODS_13.getDbId() + 1);
        dtd.GOODS_12.setDelisted(true);
        assertEquals(dtd.GOODS_12, goods);
        List<DGSTag> allTags = CollectionUtil.toList(service.getAllTags(0, Integer.MAX_VALUE));
        List<DGSTag> expectedTags = new ArrayList<>();
        expectedTags.add(dtd.TAG_11);
        expectedTags.add(dtd.TAG_12);
        long initialDbId = dtd.TAG_12.getDbId();
        for (DGSTag expectedTag : expectedTags) {
            expectedTag.setDbId(++initialDbId);
            expectedTag.setHeight(100_000);
            expectedTag.setInStockCount(expectedTag.getInStockCount() - 1);
        }
        assertTrue(allTags.containsAll(expectedTags));
    }

    @Test
    void testDelistAlreadyDelistedGoods() {
        assertThrows(IllegalStateException.class, () -> DbUtils.inTransaction(extension, (con) -> service.delistGoods(dtd.GOODS_8.getId())));
    }

    @Test
    void testDelistForUnknownTag() {
        assertThrows(IllegalStateException.class, () -> DbUtils.inTransaction(extension, (con) -> service.delistGoods(dtd.GOODS_11.getId())));
    }

    @Test
    void testChangeGoodsPrice() {
        doReturn(100_000).when(blockchain).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            service.changePrice(dtd.GOODS_5.getId(), 100);
        });
        dtd.GOODS_5.setPriceATM(100);
        dtd.GOODS_5.setHeight(100_000);
        dtd.GOODS_5.setDbId(dtd.GOODS_13.getDbId() + 1);
        DGSGoods goods = service.getGoods(dtd.GOODS_5.getId());
        assertEquals(dtd.GOODS_5, goods);
    }

    @Test
    void testChangePriceForDelistedGoods() {
        assertThrows(IllegalStateException.class, () -> service.changePrice(dtd.GOODS_8.getId(), 100));
    }

    @Test
    void testChangeQuantityFoGoodsWithZeroQuantity() {
        doReturn(100_000).when(blockchain).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            service.changeQuantity(dtd.GOODS_2.getId(), 1);
        });
        dtd.GOODS_2.setQuantity(1);
        dtd.GOODS_2.setHeight(100_000);
        dtd.GOODS_2.setDbId(dtd.GOODS_13.getDbId() + 1);
        DGSGoods goods = service.getGoods(dtd.GOODS_2.getId());
        assertEquals(dtd.GOODS_2, goods);
        List<DGSTag> tags = CollectionUtil.toList(service.getAllTags(0, Integer.MAX_VALUE));
        List<DGSTag> expectedTags = new ArrayList<>();
        expectedTags.add(dtd.TAG_4);
        expectedTags.add(dtd.TAG_5);
        expectedTags.add(dtd.TAG_6);
        long initialDbId = dtd.TAG_12.getDbId();
        for (DGSTag expectedTag : expectedTags) {
            expectedTag.setDbId(++initialDbId);
            expectedTag.setHeight(100_000);
            expectedTag.setInStockCount(expectedTag.getInStockCount() + 1);
            expectedTag.setTotalCount(expectedTag.getTotalCount() + 1);
        }
        assertTrue(tags.containsAll(expectedTags));
    }

    @Test
    void testChangeQuantityFoGoodsWithQuantityGreaterThanZero() {
        doReturn(100_000).when(blockchain).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            service.changeQuantity(dtd.GOODS_12.getId(), -1);
        });
        dtd.GOODS_12.setQuantity(2);
        dtd.GOODS_12.setHeight(100_000);
        dtd.GOODS_12.setDbId(dtd.GOODS_13.getDbId() + 1);
        DGSGoods goods = service.getGoods(dtd.GOODS_12.getId());
        assertEquals(dtd.GOODS_12, goods);

    }

    @Test
    void testChangeQuantityToNegative() {
        doReturn(100_000).when(blockchain).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            service.changeQuantity(dtd.GOODS_12.getId(), -5);
        });
        dtd.GOODS_12.setQuantity(0);
        dtd.GOODS_12.setHeight(100_000);
        dtd.GOODS_12.setDbId(dtd.GOODS_13.getDbId() + 1);
        DGSGoods goods = service.getGoods(dtd.GOODS_12.getId());
        assertEquals(dtd.GOODS_12, goods);

        List<DGSTag> expectedTags = new ArrayList<>();
        expectedTags.add(dtd.TAG_11);
        expectedTags.add(dtd.TAG_12);
        long initialDbId = dtd.TAG_12.getDbId();
        for (DGSTag expectedTag : expectedTags) {
            expectedTag.setDbId(++initialDbId);
            expectedTag.setHeight(100_000);
            expectedTag.setInStockCount(expectedTag.getInStockCount() - 1);
        }
        List<DGSTag> tags = CollectionUtil.toList(service.getAllTags(0, Integer.MAX_VALUE));
        assertTrue(tags.containsAll(expectedTags));
    }

    @Test
    void testChangeQuantityToMaxValue() {
        doReturn(100_000).when(blockchain).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            service.changeQuantity(dtd.GOODS_12.getId(), Constants.MAX_DGS_LISTING_QUANTITY + 1);
        });
        dtd.GOODS_12.setQuantity(Constants.MAX_DGS_LISTING_QUANTITY);
        dtd.GOODS_12.setHeight(100_000);
        dtd.GOODS_12.setDbId(dtd.GOODS_13.getDbId() + 1);
        DGSGoods goods = service.getGoods(dtd.GOODS_12.getId());
        assertEquals(dtd.GOODS_12, goods);
    }

    @Test
    void testChangeQuantityForDelistedGoods() {
        assertThrows(IllegalStateException.class, () -> service.changeQuantity(dtd.GOODS_8.getId(), 1));
    }

    @Test
    void testPurchaseWithTagDelisting() {
        Transaction purchaseTransaction = mock(Transaction.class);
        int height = 100_000;
        long txId = 100L;
        long senderId = 200L;

        doReturn(height).when(blockchain).getHeight();
        doReturn(500_000).when(blockchain).getLastBlockTimestamp();
        EncryptedMessageAppendix note = new EncryptedMessageAppendix(new EncryptedData("Image".getBytes(), new byte[32]), false, true);
        doReturn(note).when(purchaseTransaction).getEncryptedMessage();
        doReturn(height).when(purchaseTransaction).getHeight();
        doReturn(txId).when(purchaseTransaction).getId();
        doReturn(senderId).when(purchaseTransaction).getSenderId();

        DigitalGoodsPurchase digitalGoodsPurchase = new DigitalGoodsPurchase(dtd.GOODS_12.getId(), 3, dtd.GOODS_12.getPriceATM(), 1_000_000);
        DbUtils.inTransaction(extension, (con) -> {
            service.purchase(purchaseTransaction, digitalGoodsPurchase);
        });
        DGSPurchase expected = new DGSPurchase(dtd.PURCHASE_18.getDbId() + 1, height, txId, senderId, dtd.GOODS_12.getId(), dtd.GOODS_12.getSellerId(), 3, dtd.GOODS_12.getPriceATM(), 1_000_000, note.getEncryptedData(), 500_000, true, null, false, null, false, false, null, null, 0, 0);
        DGSPurchase purchase = service.getPurchase(txId);
        assertEquals(expected, purchase);
        purchase = service.getPendingPurchase(txId);
        assertEquals(expected, purchase);

        List<DGSTag> expectedTags = new ArrayList<>();
        expectedTags.add(dtd.TAG_11);
        expectedTags.add(dtd.TAG_12);
        long initialDbId = dtd.TAG_12.getDbId();
        for (DGSTag expectedTag : expectedTags) {
            expectedTag.setDbId(++initialDbId);
            expectedTag.setHeight(100_000);
            expectedTag.setInStockCount(expectedTag.getInStockCount() - 1);
        }
        List<DGSTag> tags = CollectionUtil.toList(service.getAllTags(0, Integer.MAX_VALUE));
        assertTrue(tags.containsAll(expectedTags));
    }

    @Test
    void testPurchaseForDelistedGoods() {
        Transaction purchaseTransaction = mock(Transaction.class);
        int height = 100_000;
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        doReturn(50L).when(purchaseTransaction).getSenderId();
        Account account = accountService.getAccount(50);
        long initialUnconfirmedBalance = account.getUnconfirmedBalanceATM();
        DigitalGoodsPurchase digitalGoodsPurchase = new DigitalGoodsPurchase(dtd.GOODS_8.getId(), 4, dtd.GOODS_8.getPriceATM(), 1_000_000);
        DbUtils.inTransaction(extension, (con) -> {
            service.purchase(purchaseTransaction, digitalGoodsPurchase);
        });
        verifyAccountBalance(50, initialUnconfirmedBalance + 4 * dtd.GOODS_8.getPriceATM(), null);
    }

    @Test
    void testPurchaseWhenPriceNotMatch() {
        Transaction purchaseTransaction = mock(Transaction.class);
        int height = 100_000;
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        doReturn(50L).when(purchaseTransaction).getSenderId();
        Account account = accountService.getAccount(50);
        long initialUnconfirmedBalance = account.getUnconfirmedBalanceATM();
        DigitalGoodsPurchase digitalGoodsPurchase = new DigitalGoodsPurchase(dtd.GOODS_12.getId(), 2, dtd.GOODS_12.getPriceATM() + 1, 1_000_000);
        DbUtils.inTransaction(extension, (con) -> service.purchase(purchaseTransaction, digitalGoodsPurchase));
        verifyAccountBalance(50, initialUnconfirmedBalance + 2 * (dtd.GOODS_12.getPriceATM() + 1), null);
    }

    @Test
    void testPurchaseWhenPriceQuantityExceedGoodsQuantity() {
        Transaction purchaseTransaction = mock(Transaction.class);
        int height = 100_000;
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        doReturn(50L).when(purchaseTransaction).getSenderId();
        Account account = accountService.getAccount(50);
        long initialUnconfirmedBalance = account.getUnconfirmedBalanceATM();
        DigitalGoodsPurchase digitalGoodsPurchase = new DigitalGoodsPurchase(dtd.GOODS_9.getId(), 2, dtd.GOODS_9.getPriceATM(), 1_000_000);
        DbUtils.inTransaction(extension, (con) -> service.purchase(purchaseTransaction, digitalGoodsPurchase));
        verifyAccountBalance(50, initialUnconfirmedBalance + 2 * (dtd.GOODS_9.getPriceATM()), null);
    }

    @Test
    void testDeliver() {
        Transaction deliverTransaction = mock(Transaction.class);
        int height = 1_000_000;
        long txId = 100L;
        long senderId = 200;
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        EncryptedMessageAppendix note = new EncryptedMessageAppendix(new EncryptedData("Image".getBytes(), new byte[32]), false, true);
        doReturn(note).when(deliverTransaction).getEncryptedMessage();
        doReturn(height).when(deliverTransaction).getHeight();
        doReturn(txId).when(deliverTransaction).getId();
        doReturn(senderId).when(deliverTransaction).getSenderId();

        long testLocalOneAPL = 100000000L;
        DigitalGoodsDelivery deliveryAttachment = new DigitalGoodsDelivery(dtd.PURCHASE_2.getId(), new EncryptedData("goods".getBytes(), new byte[32]), true, testLocalOneAPL * 2);
        DbUtils.inTransaction(extension, (con) -> {
            service.deliver(deliverTransaction, deliveryAttachment);
        });
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_2.getId());
        dtd.PURCHASE_2.setDbId(dtd.PURCHASE_18.getDbId() + 1);
        dtd.PURCHASE_2.setHeight(height);
        dtd.PURCHASE_2.setPending(false);
        dtd.PURCHASE_2.setEncryptedGoods(deliveryAttachment.getGoods(), true);
        dtd.PURCHASE_2.setDiscountATM(2 * testLocalOneAPL);
        assertEquals(dtd.PURCHASE_2, purchase);
        verifyAccountBalance(senderId, 500000000L, 550000000L);
        verifyAccountBalance(dtd.PURCHASE_2.getBuyerId(), 14725200000000L, 15024700000000L);
    }

    @Test
    void testRefund() {
        EncryptedData refundNote = new EncryptedData("Refund node".getBytes(), new byte[32]);
        int height = 1_500_000;
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        DbUtils.inTransaction(extension, (con) -> {
            service.refund(LedgerEvent.DIGITAL_GOODS_REFUND, 100, SELLER_0_ID, dtd.PURCHASE_14.getId(), 300_000_000L, new EncryptedMessageAppendix(refundNote, true, false));
        });
        dtd.PURCHASE_14.setDbId(dtd.PURCHASE_18.getDbId() + 1);
        dtd.PURCHASE_14.setHeight(1_500_000);
        dtd.PURCHASE_14.setRefundNote(refundNote);
        dtd.PURCHASE_14.setRefundATM(300_000_000);
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_14.getId());
        assertEquals(dtd.PURCHASE_14, purchase);
        verifyAccountBalance(dtd.PURCHASE_14.getSellerId(), 22700000000000L, 25099700000000L);
        verifyAccountBalance(dtd.PURCHASE_14.getBuyerId(), 14725300000000L, 15025300000000L);
    }

    @Test
    void testGoodsCount() {
        assertEquals(9, service.getGoodsCount());
    }

    @Test
    void testCountGoodsInStock() {
        assertEquals(4, service.getGoodsCountInStock());
    }

    @Test
    void testGetGoods() {
        DGSGoods goods = service.getGoods(dtd.GOODS_12.getId());
        assertEquals(dtd.GOODS_12, goods);
    }

    @Test
    void testGetAllGoods() {
        List<DGSGoods> dgsGoods = CollectionUtil.toList(service.getAllGoods(0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.GOODS_12, dtd.GOODS_4, dtd.GOODS_2, dtd.GOODS_5, dtd.GOODS_10, dtd.GOODS_8, dtd.GOODS_9, dtd.GOODS_11, dtd.GOODS_13), dgsGoods);
    }

    @Test
    void testGetAllGoodsWithPagination() {
        List<DGSGoods> dgsGoods = CollectionUtil.toList(service.getAllGoods(3, 5));
        assertEquals(List.of(dtd.GOODS_5, dtd.GOODS_10, dtd.GOODS_8), dgsGoods);
    }

    @Test
    void testGetGoodsInStock() {
        List<DGSGoods> dgsGoods = CollectionUtil.toList(service.getGoodsInStock(0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.GOODS_12, dtd.GOODS_10, dtd.GOODS_11, dtd.GOODS_13), dgsGoods);
    }

    @Test
    void testGetGoodsInStockWithPagination() {
        List<DGSGoods> dgsGoods = CollectionUtil.toList(service.getGoodsInStock(1, 2));
        assertEquals(List.of(dtd.GOODS_10, dtd.GOODS_11), dgsGoods);
    }

    @Test
    void testGetSellerGoods() {
        List<DGSGoods> goods = CollectionUtil.toList(service.getSellerGoods(SELLER_0_ID, false, 0, Integer.MAX_VALUE));
        assertIterableEquals(List.of(dtd.GOODS_5, dtd.GOODS_12, dtd.GOODS_4, dtd.GOODS_9, dtd.GOODS_11, dtd.GOODS_10, dtd.GOODS_8), goods);
        goods = CollectionUtil.toList(service.getSellerGoods(SELLER_1_ID, false, 0, Integer.MAX_VALUE));
        assertIterableEquals(List.of(dtd.GOODS_2), goods);
    }

    @Test
    void testGetSellerGoodsInStock() {
        List<DGSGoods> goods = CollectionUtil.toList(service.getSellerGoods(SELLER_0_ID, true, 0, Integer.MAX_VALUE));
        assertIterableEquals(List.of(dtd.GOODS_12, dtd.GOODS_11, dtd.GOODS_10), goods);
        goods = CollectionUtil.toList(service.getSellerGoods(SELLER_1_ID, true, 0, Integer.MAX_VALUE));
        assertIterableEquals(List.of(), goods);
    }

    @Test
    void testGetSellerGoodsWithPagination() {
        List<DGSGoods> goods = CollectionUtil.toList(service.getSellerGoods(SELLER_0_ID, false, 2, 4));
        List<DGSGoods> expected = List.of(dtd.GOODS_4, dtd.GOODS_9, dtd.GOODS_11);
        assertIterableEquals(expected, goods);
        goods = CollectionUtil.toList(service.getSellerGoods(SELLER_1_ID, false, 0, 0));
        assertIterableEquals(List.of(dtd.GOODS_2), goods);
    }

    @Test
    void testGetSellerGoodsCount() {
        int sellerGoodsCount = service.getSellerGoodsCount(SELLER_0_ID, false);
        assertEquals(7, sellerGoodsCount);
        sellerGoodsCount = service.getSellerGoodsCount(SELLER_1_ID, false);
        assertEquals(1, sellerGoodsCount);
    }

    @Test
    void testGetSellerGoodsCountInStock() {
        int sellerGoodsCount = service.getSellerGoodsCount(SELLER_0_ID, true);
        assertEquals(3, sellerGoodsCount);
        sellerGoodsCount = service.getSellerGoodsCount(SELLER_1_ID, true);
        assertEquals(0, sellerGoodsCount);
    }


    private void verifyAccountBalance(long accountId, Long unconfirmedBalance, Long balance) {
        Account account = accountService.getAccount(accountId);
        if (balance != null) {
            assertEquals(balance, account.getBalanceATM());
        }
        assertEquals(unconfirmedBalance, account.getUnconfirmedBalanceATM());
    }


}
