/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
public class DGSServiceTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    Blockchain blockchain = mock(Blockchain.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, DaoConfig.class,
            JdbiHandleFactory.class,
            GlobalSyncImpl.class,
            FullTextConfigImpl.class,
            DGSPublicFeedbackTable.class,
            DGSFeedbackTable.class,
            DGSGoodsTable.class,
            DGSTagTable.class,
            DGSPurchaseTable.class,
            DGSServiceImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();
    @Inject
    DGSService service;

    @Inject
    JdbiHandleFactory jdbiHandleFactory;

    DGSTestData dtd;

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @BeforeEach
    public void setUp() {
        dtd = new DGSTestData();
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
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_2, dtd.PURCHASE_18), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_1_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_8), dgsPurchases);
    }

    @Test
    void testGetPurchasesForSellerWhichNotExist() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(1L, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), dgsPurchases);
    }

    @Test
    void testGetSellerPurchasesWithFeedbacks() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(dgsPurchases, List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5));
    }

    @Test
    void testGetSellerCompletedPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_0_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5), dgsPurchases);
        dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_1_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_8), dgsPurchases);
    }

    @Test
    void testGetSellerPurchasesWithPagination() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getSellerPurchases(SELLER_0_ID, true, true, 1, 1));
        assertEquals(dgsPurchases, List.of(dtd.PURCHASE_14));
    }

    @Test
    void testGetSellerPurchaseCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, false, false);
        assertEquals(5, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, false, false);
        assertEquals(1, sellerPurchaseCount);
    }
    @Test
    void testGetSellerPurchaseWithFeedbacksCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, true, false);
        assertEquals(3, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, true, false);
        assertEquals(0, sellerPurchaseCount);
    }
    @Test
    void testGetSellerCompletedPurchaseCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, false, true);
        assertEquals(3, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, false, true);
        assertEquals(1, sellerPurchaseCount);
    }
    @Test
    void testGetSellerCompletedPurchaseWithFeedbacksCount() {
        int sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_0_ID, true, true);
        assertEquals(3, sellerPurchaseCount);
        sellerPurchaseCount = service.getSellerPurchaseCount(SELLER_1_ID, true, true);
        assertEquals(0, sellerPurchaseCount);
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
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, false, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_8), purchases);
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
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_8), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, false, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
    }
    @Test
    void testGetSellerBuyerPurchasesWithFeedback() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, true, false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
    }
    @Test
    void testGetSellerBuyerCompletedPurchasesWithFeedback() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, true, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_14), purchases);
    }
    @Test
    void testGetSellerBuyerPurchasesWithPagination() {
        List<DGSPurchase> purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_0_ID, false, false, 1, 2));
        assertEquals(List.of(dtd.PURCHASE_5), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_1_ID, BUYER_0_ID, false, false, 1, 1));
        assertEquals(List.of(), purchases);
        purchases = CollectionUtil.toList(service.getSellerBuyerPurchases(SELLER_0_ID, BUYER_2_ID, false, false, 1, 1));
        assertEquals(List.of(dtd.PURCHASE_2), purchases);
    }


    @Test
    void testGetSellerBuyerPurchaseCount() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_0_ID, false, false);
        assertEquals(2, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, false, false);
        assertEquals(1, purchaseCount);
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
        assertEquals(2, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, false, true);
        assertEquals(1, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_2_ID, false, true);
        assertEquals(1, purchaseCount);
    }
    @Test
    void testGetSellerBuyerPurchaseWithFeedbackCount() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_0_ID, true, false);
        assertEquals(2, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, true, false);
        assertEquals(0, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_2_ID, true, false);
        assertEquals(1, purchaseCount);
    }
    @Test
    void testGetSellerBuyerCompletedPurchaseWithFeedbackCount() {
        int purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_0_ID, BUYER_0_ID, true, true);
        assertEquals(2, purchaseCount);
        purchaseCount = service.getSellerBuyerPurchaseCount(SELLER_1_ID, BUYER_0_ID, true, true);
        assertEquals(0, purchaseCount);
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

        Block lastBlock = mock(Block.class);
        Block prevBlock = mock(Block.class);
        doReturn(dtd.PURCHASE_2.getDeadline()).when(prevBlock).getTimestamp();
        doReturn(dtd.PURCHASE_2.getDeadline() + 60).when(lastBlock).getTimestamp();
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(prevBlock).when(blockchain).getBlock(1L);

        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getExpiredPendingPurchases(lastBlock));

        assertEquals(List.of(dtd.PURCHASE_2), dgsPurchases);
    }


    @Test
    void testGetExpiredPendingPurchasesByBlockBelowPurchaseDeadline() {

        Block lastBlock = mock(Block.class);
        Block prevBlock = mock(Block.class);
        doReturn(dtd.PURCHASE_2.getDeadline() - 60).when(prevBlock).getTimestamp();
        doReturn(dtd.PURCHASE_2.getDeadline()).when(lastBlock).getTimestamp();
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(prevBlock).when(blockchain).getBlock(1L);

        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getExpiredPendingPurchases(lastBlock));

        assertEquals(List.of(), dgsPurchases);
    }
    @Test
    void testGetExpiredPendingPurchasesByBlockAbovePurcaseDeadline() {

        Block lastBlock = mock(Block.class);
        Block prevBlock = mock(Block.class);
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
        DbUtils.inTransaction(extension, (con)-> {
            service.setPending(dtd.PURCHASE_8, true);
        });
        DGSPurchase pendingPurchase = service.getPendingPurchase(dtd.PURCHASE_8.getId());
        dtd.PURCHASE_8.setDbId(dtd.PURCHASE_18.getDbId() + 1);
        assertEquals(dtd.PURCHASE_8, pendingPurchase);
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(0, Integer.MAX_VALUE));
        assertTrue(dgsPurchases.contains(dtd.PURCHASE_8));
        assertEquals(6, dgsPurchases.size());
    }

    @Test
    void testSetPendingForNewPurchase() {
        DbUtils.inTransaction(extension, (con)-> {
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
        DbUtils.inTransaction(extension, (con)-> {

            dtd.PURCHASE_16.setHeight(dtd.PURCHASE_18.getHeight() + 1000);
            dtd.PURCHASE_16.setDbId(dtd.PURCHASE_18.getDbId());

            DGSPublicFeedback publicFeedback = new DGSPublicFeedback(0L, blockchain.getHeight(), "New public feedback added", dtd.PURCHASE_16.getId());
            expected.add(publicFeedback);

            service.addPublicFeedback(dtd.PURCHASE_16, publicFeedback.getFeedback());
            assertEquals(expected, dtd.PURCHASE_16.getPublicFeedbacks());
        });
        expected.get(0).setDbId(tdCopy.PUBLIC_FEEDBACK_13.getDbId() + 1);
        expected.get(1).setDbId(tdCopy.PUBLIC_FEEDBACK_13.getDbId() + 2);
        expected.get(2).setDbId(tdCopy.PUBLIC_FEEDBACK_13.getDbId() + 3);
        List<DGSPublicFeedback> feedbacks = service.getPublicFeedbacks(dtd.PURCHASE_16);
        assertEquals(expected, feedbacks);
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_16.getId());
        assertEquals(tdCopy.PURCHASE_16.getHeight(), purchase.getHeight());
    }

    @Test
    void testAddPublicFeedbackForPurchaseWithoutPublicFeedbacks() {
        doReturn(dtd.PURCHASE_18.getHeight() + 1000).when(blockchain).getHeight();
        List<DGSPublicFeedback> expected = new ArrayList<>();
        DbUtils.inTransaction(extension, (con)-> {

            dtd.PURCHASE_2.setHeight(dtd.PURCHASE_18.getHeight() + 1000);
            dtd.PURCHASE_2.setDbId(dtd.PURCHASE_18.getDbId());

            DGSPublicFeedback publicFeedback = new DGSPublicFeedback(0L, blockchain.getHeight(), "New public feedback added", dtd.PURCHASE_2.getId());
            expected.add(publicFeedback);

            service.addPublicFeedback(dtd.PURCHASE_2, publicFeedback.getFeedback());
            assertEquals(expected, dtd.PURCHASE_2.getPublicFeedbacks());
        });
        expected.get(0).setDbId(dtd.PUBLIC_FEEDBACK_13.getDbId() + 1);
        List<DGSPublicFeedback> feedbacks = service.getPublicFeedbacks(dtd.PURCHASE_2);
        assertEquals(expected, feedbacks);
        DGSPurchase purchase = service.getPurchase(dtd.PURCHASE_2.getId());
        assertEquals(blockchain.getHeight(), purchase.getHeight());
        assertTrue(purchase.hasPublicFeedbacks());
    }






}






