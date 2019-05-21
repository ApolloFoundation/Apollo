/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import static com.apollocurrency.aplwallet.apl.data.DGSTestData.BUYER_0_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.BUYER_1_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.BUYER_2_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.SELLER_0_ID;
import static com.apollocurrency.aplwallet.apl.data.DGSTestData.SELLER_1_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
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
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
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

import java.util.List;
import javax.inject.Inject;

@EnableWeld
public class DGSServiceTest {
    @RegisterExtension
    DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
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
        assertEquals(5, purchaseCount);
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

        assertEquals(5, purchaseCount);
        int defaultPurchaseCount = service.getPurchaseCount();
        assertEquals(defaultPurchaseCount, purchaseCount);
    }

    @Test
    void testGetAllPurchases() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_8, dtd.PURCHASE_2), dgsPurchases);
    }

    @Test
    void testGetAllPurchasesWithOffset() {
        List<DGSPurchase> dgsPurchases = CollectionUtil.toList(service.getAllPurchases(2, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.PURCHASE_5, dtd.PURCHASE_8, dtd.PURCHASE_2), dgsPurchases);
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
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_8, dtd.PURCHASE_2), dgsPurchases);
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
        assertEquals(List.of(dtd.PURCHASE_16, dtd.PURCHASE_14, dtd.PURCHASE_5, dtd.PURCHASE_2), dgsPurchases);
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
        assertEquals(4, sellerPurchaseCount);
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


}






