/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.AssetTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("slow")
@EnableWeld
class AssetTableTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(Map.of("asset", List.of("name,description")));

    @Inject
    AssetTable table;
    AssetTestData td;
    AccountTestData accountTestData;

    Comparator<Asset> assetComparator = Comparator
        .comparing(Asset::getId)
        .thenComparing(Asset::getAccountId);

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AssetTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(dbExtension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
        .addBeans(MockBean.of(dbExtension.getFtl(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @BeforeEach
    void setUp() {
        td = new AssetTestData();
        accountTestData = new AccountTestData();
    }

    @Test
    void testLoad() {
        Asset asset = table.get(table.getDbKeyFactory().newKey(td.ASSET_0));
        assertNotNull(asset);
        assertEquals(td.ASSET_0, asset);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        Asset asset = table.get(table.getDbKeyFactory().newKey(td.ASSET_NEW));
        assertNull(asset);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        Asset previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ASSET_NEW));
        Asset actual = table.get(table.getDbKeyFactory().newKey(td.ASSET_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.ASSET_NEW.getAccountId(), actual.getAccountId());
        assertEquals(td.ASSET_NEW.getId(), actual.getId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        Asset previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_1));
        assertNotNull(previous);
        previous.setQuantityATU(previous.getQuantityATU() + 100);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        Asset actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertEquals(100, actual.getQuantityATU() - td.ASSET_1.getQuantityATU());
        assertEquals(previous.getQuantityATU(), actual.getQuantityATU());
        assertEquals(previous.getId(), actual.getId());
    }

    @Test
    void testDefaultSort() {
        assertNotNull(table.defaultSort());
        List<Asset> expectedAll = td.ALL_ASSETS_ORDERED_BY_ID.stream().sorted(assetComparator).collect(Collectors.toList());
        List<Asset> actualAll = toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(expectedAll, actualAll);
    }

    @Test
    void testGetAssetCount() {
        long count = table.getCount();
        assertEquals(8, count);
    }

    @Test
    void getAssetsIssuedBy() {
        List<Asset> expected = toList(table.getManyBy(
            new DbClause.LongClause("account_id", td.ASSET_1.getAccountId()), 0, 1));
        assertEquals(2, expected.size());
    }

    @Test
    void test_searchAssets() {
        List<Asset> expected = toList(table.search("This", DbClause.EMPTY_CLAUSE, 0, 3, " ORDER BY ft.score DESC "));
        assertEquals(4, expected.size());
    }
}