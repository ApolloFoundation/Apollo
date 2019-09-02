/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableWeld
class AccountAssetServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    //private DatabaseManager databaseManager = mock(DatabaseManagerImpl.class);
    private AccountAssetTable accountAssetTable = mock(AccountAssetTable.class);
    private Event<Account> accountEvent = mock(Event.class);
    private Event<AccountAsset> accountAssetEvent = mock(Event.class);


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class,
            AccountAssetServiceImpl.class
    )
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .addBeans(MockBean.of(accountAssetTable, AccountAssetTable.class))
            //.addBeans(MockBean.of(databaseManager, DatabaseManager.class, DatabaseManagerImpl.class))
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
    void addToAssetBalanceATU() {
        long quantity = 50000L;
        long assetId = 50L;
        LedgerEvent event = LedgerEvent.ASSET_DIVIDEND_PAYMENT;
        long eventId = 10L;
        long balance = Math.addExact(testData.ACC_ASS_0.getQuantityATU(), quantity);

        AccountAssetService spyService = spy(accountAssetService);

        doReturn(testData.ACC_ASS_0).when(accountAssetTable).get(any());
        spyService.addToAssetBalanceATU(testData.ACC_1, event, eventId, assetId, quantity);

        assertEquals(balance, testData.ACC_ASS_0.getQuantityATU());
        verify(spyService).update(testData.ACC_ASS_0);
        verify(accountEvent).fire(testData.ACC_1);
        verify(accountAssetEvent).fire(testData.ACC_ASS_0);
    }

    @Test
    void addToAssetBalanceATU_newAsset() {
        //ACC0
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