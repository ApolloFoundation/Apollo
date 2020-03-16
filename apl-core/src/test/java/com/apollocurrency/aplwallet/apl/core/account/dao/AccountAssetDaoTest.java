/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
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
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
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

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountAssetDaoTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, AccountAssetTable.class
    )
            .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
            .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
            .build();

    @Inject
    AccountAssetTable table;

    AccountTestData td;

    Comparator<AccountAsset> assetComparator = Comparator
            .comparing(AccountAsset::getQuantityATU, Comparator.reverseOrder())
            .thenComparing(AccountAsset::getAccountId)
            .thenComparing(AccountAsset::getAssetId);

    @BeforeEach
    void setUp() {
        td = new AccountTestData();
    }

    @Test
    void testLoad() {
        AccountAsset accountAsset = table.get(table.getDbKeyFactory().newKey(td.ACC_ASSET_0));
        assertNotNull(accountAsset);
        assertEquals(td.ACC_ASSET_0, accountAsset);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        AccountAsset accountAsset = table.get(table.getDbKeyFactory().newKey(td.newAsset));
        assertNull(accountAsset);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        AccountAsset previous = table.get(table.getDbKeyFactory().newKey(td.newAsset));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.newAsset));
        AccountAsset actual = table.get(table.getDbKeyFactory().newKey(td.newAsset));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.newAsset.getAccountId(), actual.getAccountId());
        assertEquals(td.newAsset.getAssetId(), actual.getAssetId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        AccountAsset previous = table.get(table.getDbKeyFactory().newKey(td.ACC_ASSET_0));
        assertNotNull(previous);
        previous.setUnconfirmedQuantityATU(previous.getUnconfirmedQuantityATU()+50000);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        AccountAsset actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertTrue(actual.getUnconfirmedQuantityATU()- td.ACC_ASSET_0.getUnconfirmedQuantityATU() == 50000);
        assertEquals(previous.getQuantityATU(), actual.getQuantityATU());
        assertEquals(previous.getAssetId(), actual.getAssetId());
    }

    @Test
    void testDefaultSort() {
        assertNotNull(table.defaultSort());
        List<AccountAsset> expectedAll = td.ALL_ASSETS.stream().sorted(assetComparator).collect(Collectors.toList());
        List<AccountAsset> actualAll = toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(expectedAll, actualAll);
    }

    @Test
    void testGetAssetCount() {
        long count = table.getCountByAssetId(td.ACC_ASSET_6.getAssetId());
        assertEquals(4, count);
    }

    @Test
    void testGetAssetCount_on_Height() {
        doReturn(td.ASS_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        long count = table.getCountByAssetId(td.ACC_ASSET_6.getAssetId(), td.ACC_ASSET_6.getHeight());
        assertEquals(3, count);
    }

    @Test
    void testGetAccountAssetCount() {
        long count = table.getCountByAccountId(td.ACC_ASSET_12.getAccountId());
        assertEquals(2, count);
    }

    @Test
    void testGetAccountAssetCount_on_Height() {
        doReturn(td.ASS_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        long count = table.getCountByAccountId(td.ACC_ASSET_12.getAccountId(), td.ACC_ASSET_12.getHeight());
        assertEquals(1, count);
    }

    @Test
    void testGetAccountAssets() {
        List<AccountAsset> actual = table.getByAccountId(td.ACC_ASSET_12.getAccountId(), 0, Integer.MAX_VALUE);
        assertEquals(2, actual.size());
        assertEquals(td.ACC_ASSET_12.getAssetId(), actual.get(0).getAssetId());
        assertEquals(td.ACC_ASSET_13.getAssetId(), actual.get(1).getAssetId());
    }

    @Test
    void testGetAccountAssets_on_Height() {
        doReturn(td.ASS_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        List<AccountAsset> actual = table.getByAccountId(td.ACC_ASSET_12.getAccountId(), td.ACC_ASSET_12.getHeight(), 0, Integer.MAX_VALUE);
        assertEquals(1, actual.size());
        assertEquals(td.ACC_ASSET_12.getAssetId(), actual.get(0).getAssetId());
    }

    @Test
    void testGetAssetAccounts() {
        List<AccountAsset> actual = table.getByAssetId(td.ACC_ASSET_6.getAssetId(), 0, Integer.MAX_VALUE);
        assertEquals(4, actual.size());
        List<AccountAsset> expected = td.ALL_ASSETS.stream()
                .filter(ass -> ass.getAssetId()== td.ACC_ASSET_6.getAssetId())
                .sorted(assetComparator).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testTrimDeletedRecord() {
        int rowCount = table.getRowCount();
        DbUtils.inTransaction(dbExtension, (con)-> table.trim(Integer.MAX_VALUE));
        assertEquals(rowCount, table.getRowCount());

        td.ACC_ASSET_2.setHeight(td.ACC_ASSET_2.getHeight() + 1);
        DbUtils.inTransaction(dbExtension, (con)-> table.deleteAtHeight(td.ACC_ASSET_2, td.ACC_ASSET_2.getHeight()));

        assertEquals(rowCount + 1, table.getRowCount());
        assertNull(table.get(td.ACC_ASSET_2.getDbKey()));

        DbUtils.inTransaction(dbExtension, (con)-> table.trim(td.ACC_ASSET_2.getHeight() + 1));

        assertEquals(rowCount - 1, table.getRowCount());
        assertNull(table.get(td.ACC_ASSET_2.getDbKey()));
    }

    @Test
    void testRollbackDeletedRecord() {
        int rowCount = table.getRowCount();
        td.ACC_ASSET_2.setHeight(td.ACC_ASSET_2.getHeight() + 1);
        DbUtils.inTransaction(dbExtension, (con)-> table.deleteAtHeight(td.ACC_ASSET_2, td.ACC_ASSET_2.getHeight()));
        assertEquals(rowCount + 1, table.getRowCount());
        assertNull(table.get(td.ACC_ASSET_2.getDbKey()));

        DbUtils.inTransaction(dbExtension, (con)-> table.rollback(td.ACC_ASSET_2.getHeight() - 1));

        assertEquals(3, table.getRowCount());
        td.ACC_ASSET_2.setHeight(td.ACC_ASSET_2.getHeight() - 1);
        assertEquals(td.ACC_ASSET_2, table.get(td.ACC_ASSET_2.getDbKey()));
    }

    @Test
    void testGetAssetAccounts_on_Height() {
        doReturn(td.ASS_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        List<AccountAsset> actual = table.getByAssetId(td.ACC_ASSET_6.getAssetId(), td.ACC_ASSET_6.getHeight(), 0, Integer.MAX_VALUE);
        List<AccountAsset> expected = td.ALL_ASSETS.stream()
                .filter(ass -> ass.getAssetId()== td.ACC_ASSET_6.getAssetId())
                .sorted(assetComparator).collect(Collectors.toList());
        assertEquals(3, actual.size());
        assertEquals(td.ACC_ASSET_6.getAccountId(), actual.get(0).getAccountId());
    }
}