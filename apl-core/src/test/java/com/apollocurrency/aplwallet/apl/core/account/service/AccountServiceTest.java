/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AccountServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessorImpl.class);
    private AccountTable accountTable = mock(AccountTable.class);
    private AccountGuaranteedBalanceTable accountGuaranteedBalanceTable = mock(AccountGuaranteedBalanceTable.class);
    private Event accountEvent = mock(Event.class);
    private Event ledgerEvent = mock(Event.class);
    private AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);

    private AccountServiceImpl accountService;
    private AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountService = spy(new AccountServiceImpl(
                accountTable,
                blockchain,
                blockchainConfig,
                mock(GlobalSyncImpl.class),
                accountPublicKeyService,
                accountEvent,
                ledgerEvent,
                accountGuaranteedBalanceTable
        ));
        doReturn(blockchainProcessor).when(accountService).lookupBlockchainProcessor();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testGetAccountOnHeight() {
        int height = 10000;
        long accountId = testData.PUBLIC_KEY1.getAccountId();
        DbKey dbKey = AccountTable.newKey(accountId);
        Account newAccount = new Account(accountId, height);
        Account account = accountService.getAccount(accountId, height);
        assertNull(account);

        doReturn(testData.PUBLIC_KEY1).when(accountPublicKeyService).getPublicKey(dbKey, height);
        account = accountService.getAccount(accountId, height);
        assertEquals(newAccount, account);
        assertEquals(testData.PUBLIC_KEY1, account.getPublicKey());
    }

    @Test
    void testGetAccount() {
        long accountId = testData.PUBLIC_KEY1.getAccountId();
        DbKey dbKey = AccountTable.newKey(accountId);
        Account newAccount = new Account(((LongKey) dbKey).getId(), dbKey);
        Account account = accountService.getAccount(accountId);
        assertNull(account);

        doReturn(newAccount).when(accountTable).newEntity(dbKey);
        doReturn(testData.PUBLIC_KEY1).when(accountPublicKeyService).getPublicKey(dbKey);
        account = accountService.getAccount(accountId);
        assertEquals(testData.PUBLIC_KEY1, account.getPublicKey());
    }

    @Test
    void testGetAccountByPublicKey() {
        assertNull(accountService.getAccount(testData.PUBLIC_KEY1.getPublicKey()));

        long accountId = AccountService.getId(testData.PUBLIC_KEY1.getPublicKey());
        DbKey dbKey = AccountTable.newKey(accountId);
        Account newAccount = new Account(((LongKey) dbKey).getId(), dbKey);
        doReturn(newAccount).when(accountService).getAccount(accountId);
        assertEquals(newAccount, accountService.getAccount(testData.PUBLIC_KEY1.getPublicKey()));

        //check "Duplicate key exception"
        newAccount.setPublicKey(testData.PUBLIC_KEY2);
        assertThrows(RuntimeException.class,() -> accountService.getAccount(testData.PUBLIC_KEY1.getPublicKey()));
    }

    @Test
    void testAddOrGetAccount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.addOrGetAccount(0, false));
        long accountId = testData.PUBLIC_KEY1.getAccountId();
        DbKey dbKey = AccountTable.newKey(accountId);
        Account newAccount = new Account(((LongKey) dbKey).getId(), dbKey);
        doReturn(newAccount).when(accountTable).newEntity(dbKey);
        Account account = accountService.addOrGetAccount(accountId, false);
        assertEquals(newAccount, account);
        verify(accountPublicKeyService).insertNewPublicKey(dbKey, false);
    }

    @Test
    void testUpdate_as_insert() {
        int height = 1000;
        doReturn(height).when(blockchain).getHeight();
        Account newAccount = testData.newAccount;
        newAccount.setBalanceATM(10000L);
        accountService.update(newAccount);
        verify(accountTable, times(1)).insert(newAccount);
        verify(accountTable, never()).delete(any(Account.class), eq(true), eq(height));
    }

    @Test
    void testUpdate_as_delete() {
        int height = 1000;
        doReturn(height).when(blockchain).getHeight();
        accountService.update(testData.newAccount);
        verify(accountTable, times(1)).delete(eq(testData.newAccount), eq(true), eq(height));
        verify(accountTable, never()).insert(any(Account.class));
    }

    @Test
    void getEffectiveBalanceAPL() {
    }

    @Test
    void getGuaranteedBalanceATM() {
    }

    @Test
    void getLessorsGuaranteedBalanceATM() {
    }

    @Test
    void addToBalanceATM() {
    }

    @Test
    void addToBalanceAndUnconfirmedBalanceATM() {
    }

    @Test
    void addToUnconfirmedBalanceATM() {
    }

}