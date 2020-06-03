package com.apollocurrency.aplwallet.apl.core.monetary.dao;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDividend;
import com.apollocurrency.aplwallet.apl.data.AssetDividendTestData;
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
class AssetDividendTableTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();

    @Inject
    AssetDividendTable table;
    AssetDividendTestData td;

    Comparator<AssetDividend> assetComparator = Comparator
        .comparing(AssetDividend::getDbId)
        .thenComparing(AssetDividend::getAssetId).reversed();

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AssetDividendTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @BeforeEach
    void setUp() {
        td = new AssetDividendTestData();
    }

    @Test
    void testLoad() {
        AssetDividend assetDividend = table.get(table.getDbKeyFactory().newKey(td.ASSET_DIVIDEND_0));
        assertNotNull(assetDividend);
        assertEquals(td.ASSET_DIVIDEND_0, assetDividend);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        AssetDividend assetDividend = table.get(table.getDbKeyFactory().newKey(td.ASSET_DIVIDEND_NEW));
        assertNull(assetDividend);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        AssetDividend previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_DIVIDEND_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ASSET_DIVIDEND_NEW));
        AssetDividend actual = table.get(table.getDbKeyFactory().newKey(td.ASSET_DIVIDEND_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.ASSET_DIVIDEND_NEW.getAmountATMPerATU(), actual.getAmountATMPerATU());
        assertEquals(td.ASSET_DIVIDEND_NEW.getAssetId(), actual.getAssetId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        AssetDividend previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_DIVIDEND_1));
        assertNotNull(previous);

        assertThrows(RuntimeException.class, () -> // not permitted by DB constraints
            DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous))
        );
    }

    @Test
    void getAssetDividends() {
        List<AssetDividend> actual = table.getAssetDividends( td.ASSET_DIVIDEND_2.getAssetId(), 0, 10);
        assertEquals(2, actual.size());
    }

    @Test
    void getLastDividend() {
        AssetDividend actual = table.getLastDividend( td.ASSET_DIVIDEND_2.getAssetId());
        assertEquals(td.ASSET_DIVIDEND_3, actual);
    }
}