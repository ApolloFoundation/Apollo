/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPublicFeedbackTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSPurchaseTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSTagTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSGoods;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.data.DGSTestData.SELLER_0_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Slf4j
@Testcontainers
@Tag("slow")
@EnableWeld
public class DGSServiceSearchTest {
    @Container
    public static final GenericContainer mariaDBContainer = new MariaDBContainer("mariadb:10.5")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withExposedPorts(3306)
        .withLogConsumer(new Slf4jLogConsumer(log));
    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer, Map.of("goods", List.of("name", "description", "tags")));
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
        BlockchainConfig.class,
        PropertiesHolder.class,
        BlockChainInfoServiceImpl.class, AccountServiceImpl.class, AccountTable.class
    )
        .addBeans(MockBean.of(mock(GlobalSync.class), GlobalSync.class, GlobalSyncImpl.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .addBeans(MockBean.of(extension.getFtl(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(AccountPublicKeyService.class), AccountPublicKeyServiceImpl.class, AccountPublicKeyService.class))
        .addBeans(MockBean.of(mock(AccountGuaranteedBalanceTable.class), AccountGuaranteedBalanceTable.class))
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
