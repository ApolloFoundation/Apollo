/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs;

import static com.apollocurrency.aplwallet.apl.data.DGSTestData.SELLER_0_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.account.*;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.service.*;
import com.apollocurrency.aplwallet.apl.core.app.*;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dgs.dao.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSGoods;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
@EnableWeld
public class DGSServiceSearchTest {
    @RegisterExtension
    DbExtension extension = new DbExtension(Map.of("goods", List.of("name", "description", "tags")));
    Blockchain blockchain = mock(Blockchain.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            FullTextConfigImpl.class,
            DGSPublicFeedbackTable.class,
            DGSFeedbackTable.class,
            DGSGoodsTable.class,
            DGSTagTable.class,
            DGSPurchaseTable.class,
            DGSServiceImpl.class,
            DerivedDbTablesRegistryImpl.class,
            AccountServiceImpl.class, AccountTable.class, GlobalSyncImpl.class,
            BlockchainConfig.class,
            PropertiesHolder.class,
            AccountInfoServiceImpl.class, AccountInfoTable.class,
            AccountLeaseServiceImpl.class, AccountLeaseTable.class,
            AccountAssetServiceImpl.class, AccountAssetTable.class,
            AccountPublickKeyServiceImpl.class, PublicKeyTable.class, GenesisPublicKeyTable.class,
            AccountCurrencyServiceImpl.class, AccountCurrencyTable.class,
            AccountPropertyServiceImpl.class, AccountPropertyTable.class,
            AccountFactory.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class))
            .addBeans(MockBean.of(extension.getFtl(), FullTextSearchService.class))
            .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();
    @Inject
    DGSService service;

    DGSTestData dtd;


    @BeforeEach
    public void setUp() {
        dtd = new DGSTestData();
    }


    @Test
    void testSearchGoods() {
        List<DGSGoods> goods = CollectionUtil.toList(service.searchGoods("tes*", false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.GOODS_4, dtd.GOODS_2, dtd.GOODS_10, dtd.GOODS_8), goods);
    }

    @Test
    void testSearchGoodsWithPagination() {
        List<DGSGoods> goods = CollectionUtil.toList(service.searchGoods("tes*", false, 1, 2));
        assertEquals(List.of(dtd.GOODS_2, dtd.GOODS_10), goods);
    }

    @Test
    void testSearchGoodsByTag() {
        List<DGSGoods> goods = CollectionUtil.toList(service.searchGoods("prod*", false, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.GOODS_12, dtd.GOODS_4, dtd.GOODS_2), goods);
    }

    @Test
    void testSearchSellerGoods() {
        List<DGSGoods> dgsGoods = CollectionUtil.toList(service.searchSellerGoods("bat*", SELLER_0_ID, true, 0, Integer.MAX_VALUE));
        assertEquals(List.of(dtd.GOODS_12), dgsGoods);
    }

    @Test
    void testSearchSellerGoodsWithPagination() {
        List<DGSGoods> dgsGoods = CollectionUtil.toList(service.searchSellerGoods("ta*", SELLER_0_ID, false, 1, 2));
        assertEquals(List.of(dtd.GOODS_4), dgsGoods);
    }

}
