package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountLease;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountObserverTest {

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    @Mock
    AccountService accountService;
    @Mock
    AccountLeaseService accountLeaseService;
    @Mock
    Event<AccountLease> accountLeaseEvent;
    @Mock
    AccountLedgerService accountLedgerService;

    AccountObserver observer;

    @BeforeEach
    void setUp() {
        this.observer = new AccountObserver(accountService, accountLeaseService, accountLeaseEvent, accountLedgerService);
    }

    @Test
    void onBlockApplied() {
        Block block = mock(Block.class);
        when(block.getHeight()).thenReturn(5000).thenReturn(5000).thenReturn(6000);

        AccountLease accountLease1 = new AccountLease(1L, 5000, 6000, 1L, 5000);
        AccountLease accountLease3 = new AccountLease(3L, 6000, 5000, 2L, 5000);
        accountLease3.setNextLeasingHeightFrom(5000);

        when(accountLeaseService.getLeaseChangingAccountsAtHeight(5000)).thenReturn(List.of(
            accountLease1,
            new AccountLease(2L, 6000, 5000, 2L, 5000),
            accountLease3
        ));
        Account account1 = new Account(1L, 1L, 100L, 200L, 10L, 100);
        Account account2 = new Account(2L, 1L, 100L, 200L, 10L, 100);
        when(accountService.getAccount(anyLong())).thenReturn(account1).thenReturn(account2);
        Event firedEvent = mock(Event.class);
        doNothing().doNothing().when(firedEvent).fire(any(AccountLease.class));
        AnnotationLiteral<AccountEvent> literal = AccountEventBinding.literal(AccountEventType.LEASE_STARTED);
        AnnotationLiteral<AccountEvent> literalEnded = AccountEventBinding.literal(AccountEventType.LEASE_ENDED);
        doReturn(firedEvent).when(accountLeaseEvent).select(literal);
        doReturn(firedEvent).doReturn(firedEvent).when(accountLeaseEvent).select(literalEnded);

        this.observer.onBlockApplied(block);

        verify(accountLeaseService).getLeaseChangingAccountsAtHeight(5000);
        verify(accountLeaseEvent, times(2)).select(literal);
        verify(accountLeaseEvent, times(2)).select(literalEnded);
        verify(accountService, times(3)).update(any(Account.class));
        verify(accountLeaseService, times(1)).insertLease(any(AccountLease.class));
        verify(accountLeaseService, times(1)).deleteLease(any(AccountLease.class));
    }

    @Test
    void onLedgerCommitEntries() {
        AccountLedgerEventType event = AccountLedgerEventType.COMMIT_ENTRIES;
        doNothing().when(accountLedgerService).commitEntries();

        this.observer.onLedgerCommitEntries(event);
        verify(accountLedgerService).commitEntries();
    }

    @Test
    void onLedgerClearEntries() {
        AccountLedgerEventType event = AccountLedgerEventType.COMMIT_ENTRIES;
        doNothing().when(accountLedgerService).clearEntries();

        this.observer.onLedgerClearEntries(event);
        verify(accountLedgerService).clearEntries();
    }

    @Test
    void onLogLedgerEntries() {
        LedgerEntry entry = mock(LedgerEntry.class);
        when(accountLedgerService.mustLogEntry(anyLong(), anyBoolean())).thenReturn(true).thenReturn(false);

        this.observer.onLogLedgerEntries(entry);
        this.observer.onLogLedgerEntries(entry);

        verify(accountLedgerService).logEntry(entry);
        verify(accountLedgerService, times(2)).mustLogEntry(anyLong(), anyBoolean());
    }

    @Test
    void onLogUnconfirmedLedgerEntries() {
        LedgerEntry entry = mock(LedgerEntry.class);
        when(accountLedgerService.mustLogEntry(anyLong(), anyBoolean())).thenReturn(true).thenReturn(false);

        this.observer.onLogUnconfirmedLedgerEntries(entry);
        this.observer.onLogUnconfirmedLedgerEntries(entry);

        verify(accountLedgerService).logEntry(entry);
        verify(accountLedgerService, times(2)).mustLogEntry(anyLong(), anyBoolean());
    }
}