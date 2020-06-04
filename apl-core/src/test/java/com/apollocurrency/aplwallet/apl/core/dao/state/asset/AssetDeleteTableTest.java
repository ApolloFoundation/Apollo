/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetDeleteTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDelete;
import com.apollocurrency.aplwallet.apl.data.AssetDeleteTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@EnableWeld
class AssetDeleteTableTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();

    @Inject
    AssetDeleteTable table;
    AssetDeleteTestData td;

    Comparator<AssetDelete> assetComparator = Comparator
        .comparing(AssetDelete::getDbId)
        .thenComparing(AssetDelete::getAssetId).reversed();

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AssetDeleteTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
//        .addBeans(MockBean.of(dbExtension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
//        .addBeans(MockBean.of(dbExtension.getFtl(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @BeforeEach
    void setUp() {
        td = new AssetDeleteTestData();
    }

    @Test
    void testLoad() {
        AssetDelete assetDelete = table.get(table.getDbKeyFactory().newKey(td.ASSET_DELETE_0));
        assertNotNull(assetDelete);
        assertEquals(td.ASSET_DELETE_0, assetDelete);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        AssetDelete asset = table.get(table.getDbKeyFactory().newKey(td.ASSET_DELETE_NEW));
        assertNull(asset);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        AssetDelete previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_DELETE_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ASSET_DELETE_NEW));
        AssetDelete actual = table.get(table.getDbKeyFactory().newKey(td.ASSET_DELETE_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.ASSET_DELETE_NEW.getAccountId(), actual.getAccountId());
        assertEquals(td.ASSET_DELETE_NEW.getAssetId(), actual.getAssetId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        AssetDelete previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_DELETE_1));
        assertNotNull(previous);
//        previous.setQuantityATU(previous.getQuantityATU() + 100);

        assertThrows(RuntimeException.class, () -> // not permitted by DB constraints
            DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous))
        );
    }

    @Test
    void testDefaultSort() {
        assertNotNull(table.defaultSort());
        List<AssetDelete> expectedAll = td.ALL_ASSETS_DELETE_ORDERED_BY_DBID.stream().sorted(assetComparator).collect(Collectors.toList());
        List<AssetDelete> actualAll = toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(expectedAll, actualAll);
    }

    @Test
    void testGetAssetCount() {
        long count = table.getCount();
        assertEquals(8, count);
    }

    @Test
    void getAssetsIssuedBy() {
        List<AssetDelete> expected = toList(table.getManyBy(
            new DbClause.LongClause("account_id", td.ASSET_DELETE_1.getAccountId()), 0, 10));
        assertEquals(3, expected.size());
    }

}