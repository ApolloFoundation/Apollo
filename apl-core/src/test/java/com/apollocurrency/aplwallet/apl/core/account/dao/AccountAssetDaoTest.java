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
import com.apollocurrency.aplwallet.apl.util.Constants;
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

import static com.apollocurrency.aplwallet.apl.testutil.DbUtils.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    AccountTestData testData = new AccountTestData();

    @Test
    void testLoad() {
        AccountAsset accountAsset = table.get(table.getDbKeyFactory().newKey(testData.ACC_ASS_0));
        assertNotNull(accountAsset);
        assertEquals(testData.ACC_ASS_0, accountAsset);
    }

    @Test
    void testLoad_ifNotExist_thenReturnNull() {
        AccountAsset accountAsset = table.get(table.getDbKeyFactory().newKey(testData.newAsset));
        assertNull(accountAsset);
    }

    @Test
    void testSave() {
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newAsset));
        AccountAsset actual = table.get(table.getDbKeyFactory().newKey(testData.newAsset));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(testData.newAsset.getAccountId(), actual.getAccountId());
        assertEquals(testData.newAsset.getAssetId(), actual.getAssetId());
    }

    @Test
    void testCheckAvailable_on_correct_height() {
        doReturn(720).when(blockchainProcessor).getMinRollbackHeight();
        doReturn(testData.ASS_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        assertDoesNotThrow(() -> table.checkAvailable(testData.ASS_BLOCKCHAIN_HEIGHT));
    }

    @Test
    void testCheckAvailable_on_wrong_height_LT_rollback() {
        doReturn(testData.ASS_BLOCKCHAIN_WRONG_HEIGHT + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK + 720)
                .when(blockchainProcessor).getMinRollbackHeight();
        doReturn(testData.ASS_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        assertThrows(IllegalArgumentException.class, () -> table.checkAvailable(testData.ASS_BLOCKCHAIN_WRONG_HEIGHT));
    }

    @Test
    void testCheckAvailable_on_wrong_height() {
        doReturn(720).when(blockchainProcessor).getMinRollbackHeight();
        doReturn(testData.ASS_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        assertThrows(IllegalArgumentException.class, () -> table.checkAvailable(testData.ASS_BLOCKCHAIN_WRONG_HEIGHT));
    }

    @Test
    void testUpdate_as_insert() {
        AccountAsset expected = new AccountAsset(testData.newAsset.getAccountId(), testData.newAsset.getAssetId(),
                1000L, 1000L,testData.ASS_BLOCKCHAIN_HEIGHT);
        DbUtils.inTransaction(dbExtension, (con) -> table.update(expected));
        AccountAsset actual = table.get(table.getDbKeyFactory().newKey(expected));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(expected.getAccountId(), actual.getAccountId());
        assertEquals(expected.getAssetId(), actual.getAssetId());
        assertEquals(expected.getQuantityATU(), actual.getQuantityATU());
        assertEquals(expected.getUnconfirmedQuantityATU(), actual.getUnconfirmedQuantityATU());
    }

    @Test
    void testUpdate_as_delete() {
        DbUtils.inTransaction(dbExtension, (con) -> table.update(testData.newAsset));
        AccountAsset actual = table.get(table.getDbKeyFactory().newKey(testData.newAsset));
        assertNull(actual);
    }

    @Test
    void testDefaultSort() {
        assertNotNull(table.defaultSort());
    }

    @Test
    void testGetAssetCount() {
        long count = table.getAssetCount(testData.ACC_ASS_0.getAssetId());
        assertEquals(2, count);
    }

    @Test
    void testGetAssetCount_on_Height() {
        long count = table.getAssetCount(testData.ACC_ASS_6.getAssetId(), testData.ACC_ASS_6.getHeight());
        assertEquals(4, count);
    }

    @Test
    void testGetAccountAssetCount() {
        long count = table.getAccountAssetCount(testData.ACC_ASS_12.getAccountId());
        assertEquals(2, count);
    }

    @Test
    void testGetAccountAssetCount_on_Height() {
        long count = table.getAccountAssetCount(testData.ACC_ASS_0.getAccountId(), testData.ACC_ASS_0.getHeight());
        assertEquals(1, count);
    }

    @Test
    void testGetAccountAssets() {
        List<AccountAsset> actual = toList(table.getAccountAssets(testData.ACC_ASS_12.getAccountId(), 0, Integer.MAX_VALUE));
        assertEquals(2, actual.size());
        assertEquals(testData.ACC_ASS_12.getAssetId(), actual.get(0).getAssetId());
        assertEquals(testData.ACC_ASS_13.getAssetId(), actual.get(1).getAssetId());
    }

    @Test
    void testGetAccountAssets_on_Height() {
        List<AccountAsset> actual = toList(table.getAccountAssets(testData.ACC_ASS_12.getAccountId(), testData.ACC_ASS_12.getHeight(), 0, Integer.MAX_VALUE));
        assertEquals(2, actual.size());
        assertEquals(testData.ACC_ASS_12.getAssetId(), actual.get(0).getAssetId());
        assertEquals(testData.ACC_ASS_13.getAssetId(), actual.get(1).getAssetId());
    }

    @Test
    void testGetAssetAccounts() {
        List<AccountAsset> actual = toList(table.getAssetAccounts(testData.ACC_ASS_6.getAssetId(), 0, Integer.MAX_VALUE)).stream().sorted(Comparator.comparing(AccountAsset::getAccountId)).collect(Collectors.toList());
        assertEquals(4, actual.size());
        List<AccountAsset> expected = testData.ALL_ASSETS.stream().filter(ass -> ass.getAssetId()==testData.ACC_ASS_6.getAssetId()).sorted(Comparator.comparing(AccountAsset::getAccountId)).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testGetAssetAccounts_on_Height() {
        List<AccountAsset> actual = toList(table.getAssetAccounts(testData.ACC_ASS_12.getAssetId(), testData.ACC_ASS_12.getHeight(), 0, Integer.MAX_VALUE));
        assertEquals(1, actual.size());
        assertEquals(testData.ACC_ASS_12.getAccountId(), actual.get(0).getAccountId());
    }
}