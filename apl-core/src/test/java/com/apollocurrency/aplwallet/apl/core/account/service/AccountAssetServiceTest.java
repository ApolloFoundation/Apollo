/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableWeld
class AccountAssetServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private DatabaseManager databaseManager = mock(DatabaseManagerImpl.class);
    private AccountAssetTable accountAssetTable = mock(AccountAssetTable.class);


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class,
            AccountAssetServiceImpl.class
    )
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .addBeans(MockBean.of(accountAssetTable, AccountAssetTable.class))
            .addBeans(MockBean.of(databaseManager, DatabaseManager.class, DatabaseManagerImpl.class))
            .addBeans(MockBean.of(mock(AccountService.class), AccountService.class, AccountServiceImpl.class))
            .addBeans(MockBean.of(mock(AccountLedgerService.class), AccountLedgerService.class, AccountLedgerServiceImpl.class))
            .build();

    @Inject
    AccountAssetService accountAssetService;

    AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getAssetAccounts() {
    }

    @Test
    void testGetAssetAccounts() {
    }

    @Test
    void testGetAssetAccounts1() {
    }

    @Test
    void getAssetCount() {
    }

    @Test
    void testGetAssetCount() {
    }

    @Test
    void getAssets() {
    }

    @Test
    void testGetAssets() {
    }

    @Test
    void getAccountAssetCount() {
    }

    @Test
    void testGetAccountAssetCount() {
    }

    @Test
    void getAsset() {
    }

    @Test
    void testGetAsset() {
    }

    @Test
    void testGetAsset1() {
    }

    @Test
    void getAssetBalanceATU() {
    }

    @Test
    void testGetAssetBalanceATU() {
    }

    @Test
    void testGetAssetBalanceATU1() {
    }

    @Test
    void getUnconfirmedAssetBalanceATU() {
    }

    @Test
    void addToAssetBalanceATU() {
    }

    @Test
    void addToUnconfirmedAssetBalanceATU() {
    }

    @Test
    void testUpdate_as_insert() {
        AccountAsset newAsset = new AccountAsset(testData.newAsset.getAccountId(), testData.newAsset.getAssetId(),
                1000L, 1000L,testData.ASS_BLOCKCHAIN_HEIGHT);
        accountAssetService.update(newAsset);
        verify(accountAssetTable, times(1)).insert(newAsset);
        verify(accountAssetTable, never()).delete(any(AccountAsset.class));
    }

    @Test
    void testUpdate_as_delete() {
        accountAssetService.update(testData.newAsset);
        verify(accountAssetTable, times(1)).delete(testData.newAsset);
        verify(accountAssetTable, never()).insert(any(AccountAsset.class));
    }

    @Test
    void addToAssetAndUnconfirmedAssetBalanceATU() {
    }

    @Test
    void payDividends() {
    }
}