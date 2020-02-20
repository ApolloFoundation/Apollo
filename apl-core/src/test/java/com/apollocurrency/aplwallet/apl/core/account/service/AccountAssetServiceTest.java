/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.DoubleSpendingException;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.monetary.service.AssetDividendService;
import com.apollocurrency.aplwallet.apl.core.monetary.service.AssetDividendServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableWeld
class AccountAssetServiceTest {

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private AccountAssetTable accountAssetTable = mock(AccountAssetTable.class);
    private Event accountEvent = mock(Event.class);
    private Event accountAssetEvent = mock(Event.class);
    private Event ledgerEvent = mock(Event.class);
    private AccountService accountService = mock(AccountService.class);
    private AssetDividendService assetDividendService = mock(AssetDividendServiceImpl.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class
    )
            .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
            .build();

    AccountAssetService accountAssetService;

    AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        accountAssetService = spy(new AccountAssetServiceImpl(
                blockchain,
                accountAssetTable,
                accountService,
                accountEvent,
                accountAssetEvent,
                ledgerEvent,
                assetDividendService)
        );
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
        long balance = Math.addExact(testData.ACC_ASSET_0.getQuantityATU(), quantity);
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
        Event firedEventAss = mock(Event.class);
        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.ASSET_BALANCE));
        doReturn(firedEventAss).when(accountAssetEvent).select(literal(AccountEventType.ASSET_BALANCE));
        doReturn(testData.ACC_ASSET_0).when(accountAssetTable).get(any());
        accountAssetService.addToAssetBalanceATU(testData.ACC_1, event, eventId, assetId, quantity);

        assertEquals(balance, testData.ACC_ASSET_0.getQuantityATU());
        verify(accountAssetService).update(testData.ACC_ASSET_0);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventAss).fire(testData.ACC_ASSET_0);
        verify(firedEventLedger, times(1)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToAssetBalanceATU_newAsset() {
        long quantity = 50000L;
        long assetId = 50L;
        LedgerEvent event = LedgerEvent.ASSET_DIVIDEND_PAYMENT;
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
        AccountAsset expectedNewAsset = new AccountAsset(testData.ACC_0.getId(), assetId, quantity, 0, blockchain.getHeight());

        Event firedEventAcc = mock(Event.class);
        Event firedEventAss = mock(Event.class);
        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.ASSET_BALANCE));
        doReturn(firedEventAss).when(accountAssetEvent).select(literal(AccountEventType.ASSET_BALANCE));
        doReturn(null).when(accountAssetTable).get(any());
        accountAssetService.addToAssetBalanceATU(testData.ACC_0, event, eventId, assetId, quantity);

        verify(accountAssetService).update(expectedNewAsset);
        verify(firedEventAcc).fire(testData.ACC_0);
        verify(firedEventAss).fire(expectedNewAsset);
        verify(firedEventLedger, times(1)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToUnconfirmedAssetBalanceATU_expectedException() {
        long quantity = 50000L;
        long assetId = 50L;
        LedgerEvent event = LedgerEvent.ASSET_DIVIDEND_PAYMENT;
        long eventId = 10L;

        doReturn(testData.ACC_ASSET_0).when(accountAssetTable).get(any());
        assertThrows(DoubleSpendingException.class, () ->
                accountAssetService.addToUnconfirmedAssetBalanceATU(testData.ACC_1, event, eventId, assetId, quantity));
    }

    @Test
    void addToUnconfirmedAssetBalanceATU() {
        long quantity = -50000L;
        long assetId = 50L;
        LedgerEvent event = LedgerEvent.ASSET_DIVIDEND_PAYMENT;
        long eventId = 10L;
        long balance = Math.addExact(testData.ACC_ASSET_3.getUnconfirmedQuantityATU(), quantity);
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
        Event firedEventAss = mock(Event.class);
        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE));
        doReturn(firedEventAss).when(accountAssetEvent).select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE));
        doReturn(testData.ACC_ASSET_3).when(accountAssetTable).get(any());
        accountAssetService.addToUnconfirmedAssetBalanceATU(testData.ACC_1, event, eventId, assetId, quantity);

        assertEquals(balance, testData.ACC_ASSET_3.getUnconfirmedQuantityATU());
        verify(accountAssetService).update(testData.ACC_ASSET_3);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventAss).fire(testData.ACC_ASSET_3);
        verify(firedEventLedger, times(1)).fire(any(LedgerEntry.class));
    }

    @Test
    void addToUnconfirmedAssetBalanceATU_newAssetWithException() {
        long quantity = -50000L;
        long assetId = 50L;
        LedgerEvent event = LedgerEvent.ASSET_DIVIDEND_PAYMENT;
        long eventId = 10L;

        doReturn(null).when(accountAssetTable).get(any());

        assertThrows(DoubleSpendingException.class,() ->
                accountAssetService.addToUnconfirmedAssetBalanceATU(testData.ACC_0, event, eventId, assetId, quantity));

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
        long quantity = -50000L;
        long assetId = 50L;
        LedgerEvent event = LedgerEvent.ASSET_DIVIDEND_PAYMENT;
        long eventId = 10L;
        long balance = Math.addExact(testData.ACC_ASSET_3.getQuantityATU(), quantity);
        long unconfirmedBalance = Math.addExact(testData.ACC_ASSET_3.getUnconfirmedQuantityATU(), quantity);
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
        Event firedEventAss = mock(Event.class);
        Event firedEventAccUnconfirmed = mock(Event.class);
        Event firedEventAssUnconfirmed = mock(Event.class);

        doReturn(firedEventAcc).when(accountEvent).select(literal(AccountEventType.ASSET_BALANCE));
        doReturn(firedEventAccUnconfirmed).when(accountEvent).select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE));

        doReturn(firedEventAss).when(accountAssetEvent).select(literal(AccountEventType.ASSET_BALANCE));
        doReturn(firedEventAssUnconfirmed).when(accountAssetEvent).select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE));
        doReturn(testData.ACC_ASSET_3).when(accountAssetTable).get(any());
        accountAssetService.addToAssetAndUnconfirmedAssetBalanceATU(testData.ACC_1, event, eventId, assetId, quantity);

        assertEquals(balance, testData.ACC_ASSET_3.getQuantityATU());
        assertEquals(unconfirmedBalance, testData.ACC_ASSET_3.getUnconfirmedQuantityATU());
        verify(accountAssetService).update(testData.ACC_ASSET_3);
        verify(firedEventAcc).fire(testData.ACC_1);
        verify(firedEventAccUnconfirmed).fire(testData.ACC_1);
        verify(firedEventAss).fire(testData.ACC_ASSET_3);
        verify(firedEventAssUnconfirmed).fire(testData.ACC_ASSET_3);
        verify(firedEventLedger, times(2)).fire(any(LedgerEntry.class));
    }

    @Test
    void payDividends() {
        long transactionId = 10_000_000L;
        long amountATMPerATU = 100L;
        final int height = 115621;
        Comparator<AccountAsset> assetComparator = Comparator
                .comparing(AccountAsset::getQuantityATU, Comparator.reverseOrder())
                .thenComparing(AccountAsset::getAccountId)
                .thenComparing(AccountAsset::getAssetId);

        ColoredCoinsDividendPayment attachment = new ColoredCoinsDividendPayment(testData.ACC_ASSET_6.getAssetId(), height, amountATMPerATU);

        List<AccountAsset> expected = testData.ALL_ASSETS.stream()
                .filter(ass -> ass.getAssetId()==testData.ACC_ASSET_6.getAssetId())
                .sorted(assetComparator).collect(Collectors.toList());

        long numCount = expected.size();
        long totalDivident = expected.stream()
                .collect(Collectors.summingLong(
                        accountAsset -> Math.multiplyExact(accountAsset.getQuantityATU(), amountATMPerATU)));

        doReturn(testData.ACC_0).when(accountService).getAccount(any(long.class));
        doReturn(expected).when(accountAssetTable).getByAssetId(testData.ACC_ASSET_6.getAssetId(), height, 0, -1);

        accountAssetService.payDividends(testData.ACC_6, transactionId, attachment);
        verify(accountService, times(4)).addToBalanceAndUnconfirmedBalanceATM(any(Account.class), eq(LedgerEvent.ASSET_DIVIDEND_PAYMENT), eq(transactionId), any(long.class));
        verify(accountService).addToBalanceATM(testData.ACC_6, LedgerEvent.ASSET_DIVIDEND_PAYMENT, transactionId, -totalDivident);
        verify(assetDividendService).addAssetDividend(eq(transactionId), any(ColoredCoinsDividendPayment.class), eq(totalDivident), eq(numCount));
    }
}