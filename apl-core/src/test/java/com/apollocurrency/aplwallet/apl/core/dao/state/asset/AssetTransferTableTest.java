/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.util.Comparator;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;
import com.apollocurrency.aplwallet.apl.data.AssetTransferTestData;
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
class AssetTransferTableTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();

    @Inject
    AssetTransferTable table;
    AssetTransferTestData td;

    Comparator<AssetTransfer> assetComparator = Comparator
        .comparing(AssetTransfer::getDbId)
        .thenComparing(AssetTransfer::getAssetId).reversed();

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AssetTransferTable.class
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
        td = new AssetTransferTestData();
    }

    @Test
    void testLoad() {
        AssetTransfer assetDelete = table.get(table.getDbKeyFactory().newKey(td.ASSET_TRANSFER_0));
        assertNotNull(assetDelete);
        assertEquals(td.ASSET_TRANSFER_0, assetDelete);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        AssetTransfer asset = table.get(table.getDbKeyFactory().newKey(td.ASSET_TRANSFER_NEW));
        assertNull(asset);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        AssetTransfer previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_TRANSFER_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.ASSET_TRANSFER_NEW));
        AssetTransfer actual = table.get(table.getDbKeyFactory().newKey(td.ASSET_TRANSFER_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.ASSET_TRANSFER_NEW.getId(), actual.getId());
        assertEquals(td.ASSET_TRANSFER_NEW.getAssetId(), actual.getAssetId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        AssetTransfer previous = table.get(table.getDbKeyFactory().newKey(td.ASSET_TRANSFER_1));
        assertNotNull(previous);
//        previous.setQuantityATU(previous.getQuantityATU() + 100);

        assertThrows(RuntimeException.class, () -> // not permitted by DB constraints
            DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous))
        );
    }


    @Test
    void getAccountAssetTransfers() {
    }

    @Test
    void testGetAccountAssetTransfers() {
    }
}