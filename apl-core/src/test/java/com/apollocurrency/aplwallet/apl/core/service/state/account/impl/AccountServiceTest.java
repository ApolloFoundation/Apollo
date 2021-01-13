/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.impl.BlockChainInfoServiceImpl;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventBinding.literal;
import static com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl.EFFECTIVE_BALANCE_CONFIRMATIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import static org.mockito.Mockito.when;

class AccountServiceTest {

    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private AccountTable accountTable = mock(AccountTable.class);
    private AccountGuaranteedBalanceTable accountGuaranteedBalanceTable = mock(AccountGuaranteedBalanceTable.class);
    private Event accountEvent = mock(Event.class);
    private Event ledgerEvent = mock(Event.class);
    private AccountPublicKeyService accountPublicKeyService = mock(AccountPublicKeyService.class);
    private Blockchain blockchain = mock(Blockchain.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    private BlockChainInfoService blockChainInfoService = spy(new BlockChainInfoServiceImpl(
        blockchain, blockchainProcessor
    ));

    private AccountServiceImpl accountService;
    private AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountService = spy(new AccountServiceImpl(
            accountTable,
            blockchainConfig,
            mock(GlobalSyncImpl.class),
            accountPublicKeyService,
            accountEvent,
            ledgerEvent,
            accountGuaranteedBalanceTable,
            blockChainInfoService
        ));
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
        newAccount.setPublicKey(testData.PUBLIC_KEY1);
        Account account = accountService.getAccount(accountId, height);
        assertNull(account);

        doReturn(testData.PUBLIC_KEY1).when(accountPublicKeyService).getByHeight(accountId, height);
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

        doReturn(testData.PUBLIC_KEY1).when(accountPublicKeyService).getPublicKey(accountId);
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
        assertThrows(RuntimeException.class, () -> accountService.getAccount(testData.PUBLIC_KEY1.getPublicKey()));
    }

    @Test
    void testAddOrGetAccount() {
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount(0));
        long accountId = testData.PUBLIC_KEY1.getAccountId();
        DbKey dbKey = AccountTable.newKey(accountId);
        Account newAccount = new Account(((LongKey) dbKey).getId(), dbKey);
        Account account = accountService.createAccount(accountId);
        assertEquals(newAccount, account);
    }

    @Test
    void testUpdate_as_insert() {
        int height = 1000;
        doReturn(height).when(blockChainInfoService).getHeight();
        Account newAccount = testData.newAccount;
        newAccount.setBalanceATM(10000L);
        accountService.update(newAccount);
        verify(accountTable, times(1)).insert(newAccount);
        verify(accountTable, never()).delete(any(Account.class), eq(height));
    }

    @Test
    void getEffectiveBalanceAPLByGenesisAccount() {
        boolean lock = false;
        int height = EFFECTIVE_BALANCE_CONFIRMATIONS - 1;
        when(blockchainConfig.getOneAPL()).thenReturn(100000000L);
        assertEquals(0L, accountService.getEffectiveBalanceAPL(testData.ACC_0, height, lock));

        doReturn(testData.ACC_0).when(accountService).getAccount(testData.ACC_0.getId(), 0);
        assertEquals(testData.ACC_0.getBalanceATM() / blockchainConfig.getOneAPL()
            , accountService.getEffectiveBalanceAPL(testData.ACC_0, height, lock));
    }

    @Test
    void getEffectiveBalanceAPL() {
        boolean lock = false;
        int height = testData.ACC_7.getHeight();
        int blockchainHeight = height + 1000;
        long balance = 0;
        long lessorsBalance = 10000L;
        long guaranteedBalance = 50000L;
        when(blockchainConfig.getOneAPL()).thenReturn(100000000L);
        doReturn(blockchainHeight).when(blockChainInfoService).getHeight();
        assertEquals(0, accountService.getEffectiveBalanceAPL(testData.ACC_0, height, lock));

        doReturn(testData.PUBLIC_KEY1).when(accountPublicKeyService).getPublicKey(testData.ACC_0.getId());
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        doReturn(lessorsBalance).when(accountService).getLessorsGuaranteedBalanceATM(testData.ACC_0, height);
        doReturn(guaranteedBalance).when(accountService).getGuaranteedBalanceATM(testData.ACC_0, 1440, height);
        balance = accountService.getEffectiveBalanceAPL(testData.ACC_0, height, lock);
        assertEquals(0, balance);//the balance is less then MIN_FORGING_BALANCE_ATM

        doReturn(lessorsBalance * blockchainConfig.getOneAPL()).when(accountService).getLessorsGuaranteedBalanceATM(testData.ACC_0, height);
        doReturn(guaranteedBalance * blockchainConfig.getOneAPL()).when(accountService).getGuaranteedBalanceATM(testData.ACC_0, 1440, height);
        balance = accountService.getEffectiveBalanceAPL(testData.ACC_0, height, lock);
        assertEquals(lessorsBalance + guaranteedBalance, balance);
    }

    @Test
    void getGuaranteedBalanceATM() {
        long subBalance = 50000L;
        int numberOfConfirmation = 1440;
        int currentHeight = testData.ACC_GUARANTEE_BALANCE_HEIGHT_MAX;
        doReturn(10).when(blockChainInfoService).getHeight();
        assertThrows(IllegalArgumentException.class, () -> accountService.getGuaranteedBalanceATM(testData.ACC_1, numberOfConfirmation, currentHeight));
        doReturn(currentHeight + 10).when(blockChainInfoService).getHeight();

        doReturn(null).when(accountGuaranteedBalanceTable).getSumOfAdditions(testData.ACC_1.getId(), currentHeight - numberOfConfirmation, currentHeight);
        long sum = accountService.getGuaranteedBalanceATM(testData.ACC_1, numberOfConfirmation, currentHeight);
        assertEquals(testData.ACC_1.getBalanceATM(), sum);

        doReturn(subBalance).when(accountGuaranteedBalanceTable).getSumOfAdditions(testData.ACC_1.getId(), currentHeight - numberOfConfirmation, currentHeight);
        sum = accountService.getGuaranteedBalanceATM(testData.ACC_1, numberOfConfirmation, currentHeight);
        assertEquals(testData.ACC_1.getBalanceATM() - subBalance, sum);
    }

    @Test
    void getLessorsGuaranteedBalanceATM() {
        int height = testData.ACC_7.getHeight();
        int blockchainHeight = height + 1000;
        doReturn(blockchainHeight).when(blockChainInfoService).getHeight();
        List<Account> lessorsList = List.of(testData.ACC_1, testData.ACC_7);
        doReturn(lessorsList).when(accountService).getLessors(testData.ACC_0, height);
        long lessorAdditionsSum = 500000L;
        Map<Long, Long> lessorAdditions = new HashMap<>();
        lessorAdditions.put(testData.ACC_1.getId(), lessorAdditionsSum);

        doReturn(lessorAdditions).when(accountGuaranteedBalanceTable).getLessorsAdditions(
            lessorsList.stream().map(Account::getId).collect(Collectors.toList()),
            height, blockchainHeight);
        long expectedBalance = lessorsList.stream().mapToLong(Account::getBalanceATM).sum() - lessorAdditionsSum;
        long lessorsBalance = accountService.getLessorsGuaranteedBalanceATM(testData.ACC_0, height);

        assertEquals(expectedBalance, lessorsBalance);
    }

    @Test
    void addToBalanceATM() {
        Account account = testData.ACC_0;
        LedgerEvent event = LedgerEvent.ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
        long eventId = 10L;
        long amount = 10000L;
        long fee = 5000L;
        Event firedEvent = mock(Event.class);
        Event firedEventLedger = mock(Event.class);
        int height = 100_000;
        Block lastBlock = mock(Block.class);
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockChainInfoService).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockChainInfoService).getHeight();

        doReturn(firedEvent).when(accountEvent).select(literal(AccountEventType.BALANCE));

        //when amount=0;
        accountService.addToBalanceATM(account, event, eventId, 0, 0);
        verify(accountService, never()).update(account);
        verify(firedEvent, never()).fire(account);
        verify(firedEventLedger, never()).fire(any(LedgerEntry.class));

        //when amount .GT. 0
        long expectedBalance = account.getBalanceATM() + amount + fee;
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY));
        accountService.addToBalanceATM(account, event, eventId, amount, fee);
        assertEquals(expectedBalance, account.getBalanceATM());
        verify(accountGuaranteedBalanceTable).addToGuaranteedBalanceATM(account.getId(), amount + fee, height);
        verify(accountService).update(account);
        verify(firedEvent).fire(account);
        verify(firedEventLedger, times(2)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToBalanceAndUnconfirmedBalanceATM() {
        Account account = testData.ACC_0;
        LedgerEvent event = LedgerEvent.ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
        long eventId = 10L;
        long amount = 10000L;
        long fee = 5000L;
        Event firedEvent = mock(Event.class);
        Event firedEventLedger = mock(Event.class);
        int height = 100_000;
        Block lastBlock = mock(Block.class);
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockChainInfoService).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockChainInfoService).getHeight();

        doReturn(firedEvent).when(accountEvent).select(literal(AccountEventType.BALANCE));
        doReturn(firedEvent).when(accountEvent).select(literal(AccountEventType.UNCONFIRMED_BALANCE));

        //when amount=0;
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, event, eventId, 0, 0);
        verify(accountService, never()).update(account);
        verify(firedEvent, never()).fire(account);
        verify(firedEventLedger, never()).fire(any(LedgerEntry.class));

        //when amount .GT. 0
        long totalAmount = amount + fee;
        long expectedBalance = account.getBalanceATM() + totalAmount;
        long expectedUnconfirmedBalance = account.getUnconfirmedBalanceATM() + totalAmount;
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY));
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY));
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, event, eventId, amount, fee);
        assertEquals(expectedBalance, account.getBalanceATM());
        assertEquals(expectedUnconfirmedBalance, account.getUnconfirmedBalanceATM());
        verify(accountGuaranteedBalanceTable).addToGuaranteedBalanceATM(account.getId(), amount + fee, height);
        verify(accountService).update(account);
        verify(firedEvent, times(2)).fire(account);
        verify(firedEventLedger, times(4)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToUnconfirmedBalanceATM() {
        Account account = testData.ACC_0;
        LedgerEvent event = LedgerEvent.ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
        long eventId = 10L;
        long amount = 10000L;
        long fee = 5000L;
        Event firedEvent = mock(Event.class);
        Event firedEventLedger = mock(Event.class);
        int height = 100_000;
        Block lastBlock = mock(Block.class);
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockChainInfoService).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockChainInfoService).getHeight();

        doReturn(firedEvent).when(accountEvent).select(literal(AccountEventType.UNCONFIRMED_BALANCE));

        //when amount=0;
        accountService.addToUnconfirmedBalanceATM(account, event, eventId, 0, 0);
        verify(accountService, never()).update(account);
        verify(firedEvent, never()).fire(account);
        verify(firedEventLedger, never()).fire(any(LedgerEntry.class));

        //when amount .GT. 0
        long totalAmount = amount + fee;
        long expectedUnconfirmedBalance = account.getUnconfirmedBalanceATM() + totalAmount;
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY));
        accountService.addToUnconfirmedBalanceATM(account, event, eventId, amount, fee);
        assertEquals(expectedUnconfirmedBalance, account.getUnconfirmedBalanceATM());
        verify(accountService).update(account);
        verify(firedEvent, times(1)).fire(account);
        verify(firedEventLedger, times(2)).fire(any(LedgerEntry.class));
    }

    @Test
    void testCheckAvailable_on_correct_height() {
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        doReturn(testData.ACC_BLOCKCHAIN_HEIGHT).when(blockChainInfoService).getHeight();
        when(blockchain.getHeight()).thenReturn(testData.ACC_BLOCKCHAIN_HEIGHT);
        when(accountTable.isMultiversion()).thenReturn(true);
        assertDoesNotThrow(() -> accountService.checkAvailable(testData.ACC_BLOCKCHAIN_HEIGHT));
    }

    @Test
    void testCheckAvailable_on_wrong_height() {
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        when(accountTable.isMultiversion()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> accountService.checkAvailable(testData.ACC_BLOCKCHAIN_WRONG_HEIGHT));
    }
}