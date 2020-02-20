/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.DoubleSpendingException;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AccountCurrencyServiceTest {
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private AccountCurrencyTable accountCurrencyTable = mock(AccountCurrencyTable.class);
    private Event accountEvent = mock(Event.class);
    private Event accountCurrencyEvent = mock(Event.class);
    private Event ledgerEvent = mock(Event.class);

    private AccountCurrencyService accountCurrencyService;
    private AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountCurrencyService = spy(new AccountCurrencyServiceImpl(
                blockchain,
                accountCurrencyTable,
                ledgerEvent,
                accountEvent,
                accountCurrencyEvent
                ));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void addToCurrencyUnits() {
        long units = 50000L;
        long currencyId = testData.CUR_0.getCurrencyId();
        LedgerEvent event = LedgerEvent.CURRENCY_TRANSFER;
        long eventId = 10L;
        long balance = Math.addExact(testData.CUR_0.getUnits(), units);
        int height = 100_000;
        Event firedEventLedger = mock(Event.class);
        Block lastBlock = mock(Block.class);
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY));
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY));

        Event firedEventAcc = mock(Event.class);
        Event firedEventCurr = mock(Event.class);
        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.CURRENCY_BALANCE));
        doReturn(firedEventCurr).when(accountCurrencyEvent).select(literal(AccountEventType.CURRENCY_BALANCE));
        doReturn(testData.CUR_0).when(accountCurrencyTable).get(any());

        accountCurrencyService.addToCurrencyUnits(testData.ACC_1, event, eventId, currencyId, units);

        assertEquals(balance, testData.CUR_0.getUnits());
        verify(accountCurrencyService).update(testData.CUR_0);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventCurr).fire(testData.CUR_0);
        verify(firedEventLedger, times(1)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToCurrencyUnits_newCurrency() {
        long units = 50000L;
        long currencyId = testData.CUR_0.getCurrencyId();
        LedgerEvent event = LedgerEvent.CURRENCY_TRANSFER;
        long eventId = 10L;
        int height = 100_000;
        Event firedEventLedger = mock(Event.class);
        Block lastBlock = mock(Block.class);
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY));
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY));
        AccountCurrency expectedNewCurrency = new AccountCurrency(testData.ACC_0.getId(), currencyId, units, 0, blockchain.getHeight());

        Event firedEventAcc = mock(Event.class);
        Event firedEventCurr = mock(Event.class);
        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.CURRENCY_BALANCE));
        doReturn(firedEventCurr).when(accountCurrencyEvent).select(literal(AccountEventType.CURRENCY_BALANCE));
        doReturn(null).when(accountCurrencyTable).get(any());

        accountCurrencyService.addToCurrencyUnits(testData.ACC_0, event, eventId, currencyId, units);

        verify(accountCurrencyService).update(expectedNewCurrency);
        verify(firedEventAcc).fire(testData.ACC_0);
        verify(firedEventCurr).fire(expectedNewCurrency);
        verify(firedEventLedger, times(1)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToUnconfirmedCurrencyUnits_expectedException() {
        long units = 50000L;
        long currencyId = testData.CUR_0.getCurrencyId();
        LedgerEvent event = LedgerEvent.CURRENCY_TRANSFER;
        long eventId = 10L;

        doReturn(testData.CUR_0).when(accountCurrencyTable).get(any());
        assertThrows(DoubleSpendingException.class, () ->
                accountCurrencyService.addToUnconfirmedCurrencyUnits(testData.ACC_1, event, eventId, currencyId, units));

    }

    @Test
    void addToUnconfirmedCurrencyUnits() {
        long units = -50000L;
        long currencyId = 50L;
        LedgerEvent event = LedgerEvent.CURRENCY_TRANSFER;
        long eventId = 10L;
        long balance = Math.addExact(testData.CUR_8.getUnconfirmedUnits(), units);
        int height = 100_000;
        Event firedEventLedger = mock(Event.class);
        Block lastBlock = mock(Block.class);
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY));
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY));

        Event firedEventAcc = mock(Event.class);
        Event firedEventCurr = mock(Event.class);
        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE));
        doReturn(firedEventCurr).when(accountCurrencyEvent).select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE));
        doReturn(testData.CUR_8).when(accountCurrencyTable).get(any());
        accountCurrencyService.addToUnconfirmedCurrencyUnits(testData.ACC_1, event, eventId, currencyId, units);

        assertEquals(balance, testData.CUR_8.getUnconfirmedUnits());
        verify(accountCurrencyService).update(testData.CUR_8);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventCurr).fire(testData.CUR_8);
        verify(firedEventLedger, times(1)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToUnconfirmedCurrencyUnits_newCurrencyWithException() {
        long units = -50000L;
        long currencyId = 50L;
        LedgerEvent event = LedgerEvent.CURRENCY_TRANSFER;
        long eventId = 10L;

        doReturn(null).when(accountCurrencyTable).get(any());

        assertThrows(DoubleSpendingException.class, () ->
                accountCurrencyService.addToUnconfirmedCurrencyUnits(testData.ACC_1, event, eventId, currencyId, units));
    }

    @Test
    void addToCurrencyAndUnconfirmedCurrencyUnits() {
        long units = 50000L;
        long currencyId = testData.CUR_0.getCurrencyId();
        LedgerEvent event = LedgerEvent.CURRENCY_TRANSFER;
        long eventId = 10L;
        long balance = Math.addExact(testData.CUR_0.getUnits(), units);
        long unconfirmedBalance = Math.addExact(testData.CUR_0.getUnconfirmedUnits(), units);
        int height = 100_000;
        Event firedEventLedger = mock(Event.class);
        Block lastBlock = mock(Block.class);
        doReturn(1L).when(lastBlock).getPreviousBlockId();
        doReturn(lastBlock).when(blockchain).getLastBlock();
        doReturn(height).when(lastBlock).getHeight();
        doReturn(height).when(blockchain).getHeight();
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY));
        doReturn(firedEventLedger).when(ledgerEvent).select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY));

        Event firedEventAcc = mock(Event.class);
        Event firedEventCurr = mock(Event.class);
        Event firedEventAccUnconfirmed = mock(Event.class);
        Event firedEventCurrUnconfirmed = mock(Event.class);

        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.CURRENCY_BALANCE));
        doReturn(firedEventAccUnconfirmed).when(accountEvent).select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE));

        doReturn(firedEventCurr).when(accountCurrencyEvent).select(literal(AccountEventType.CURRENCY_BALANCE));
        doReturn(firedEventCurrUnconfirmed).when(accountCurrencyEvent).select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE));

        doReturn(testData.CUR_0).when(accountCurrencyTable).get(any());

        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(testData.ACC_1, event, eventId, currencyId, units);

        assertEquals(balance, testData.CUR_0.getUnits());
        assertEquals(unconfirmedBalance, testData.CUR_0.getUnconfirmedUnits());
        verify(accountCurrencyService).update(testData.CUR_0);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventAccUnconfirmed).fire(testData.ACC_1);
        verify(firedEventCurr).fire(testData.CUR_0);
        verify(firedEventCurrUnconfirmed).fire(testData.CUR_0);

        verify(firedEventLedger, times(2)).fire(any(LedgerEntry.class));
    }

    @Test
    void testUpdate_as_insert() {
        AccountCurrency newCurrency = new AccountCurrency(
                testData.newCurrency.getAccountId(), testData.newCurrency.getCurrencyId(),
                1000L, 1000L,testData.CUR_BLOCKCHAIN_HEIGHT);
        accountCurrencyService.update(newCurrency);
        verify(accountCurrencyTable, times(1)).insert(newCurrency);
        verify(accountCurrencyTable, never()).delete(any(AccountCurrency.class));
    }

    @Test
    void testUpdate_as_delete() {
        accountCurrencyService.update(testData.newCurrency);
        verify(accountCurrencyTable, times(1)).delete(testData.newCurrency);
        verify(accountCurrencyTable, never()).insert(any(AccountCurrency.class));
    }

}
