package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountCurrencyBalanceObserverTest {

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    @Mock
    FundingMonitorService fundingMonitorService;
    private AccountCurrencyBalanceObserver observer;

    @BeforeEach
    void setUp() {
        observer = new AccountCurrencyBalanceObserver(fundingMonitorService);
    }

    @Test
    void onAccountCurrencyBalance_stoppedMonitor() {
        AccountCurrency accountCurrency = new AccountCurrency(1L, 1L, 100L, 200L, 100);
        when(fundingMonitorService.isStopped()).thenReturn(true);

        observer.onAccountCurrencyBalance(accountCurrency);

        verify(fundingMonitorService).isStopped();
    }

    @Test
    void onAccountCurrencyBalance() {
        AccountCurrency accountCurrency = new AccountCurrency(1L, 1L, 100L, 200L, 100);
        when(fundingMonitorService.getMonitors()).thenReturn(List.of());
        byte[] keySeed = Crypto.getKeySeed(new byte[]{0, 1, 2, 3});
        MonitoredAccount monAcc1 = new MonitoredAccount(1L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop",
                100L, 1000L, 10, accountCurrency.getAccountId(), keySeed),
            100L, 1000L, 10
        );
        MonitoredAccount monAcc2 = new MonitoredAccount(2L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop",
                100L, 1000L, 10, accountCurrency.getAccountId(), keySeed),
            100L, 1000L, 10
        );
        when(fundingMonitorService.getMonitoredAccountListById(accountCurrency.getAccountId())).thenReturn(
            List.of(
                monAcc1,
                monAcc2
            )
        );
        when(fundingMonitorService.containsPendingEvent(any(MonitoredAccount.class))).thenReturn(false).thenReturn(false);
        lenient().when(fundingMonitorService.addPendingEvent(any(MonitoredAccount.class))).thenReturn(true).thenReturn(true);

        observer.onAccountCurrencyBalance(accountCurrency);

        verify(fundingMonitorService).getMonitors();
        verify(fundingMonitorService, times(2)).containsPendingEvent(any(MonitoredAccount.class));
        verify(fundingMonitorService, times(2)).addPendingEvent(any(MonitoredAccount.class));
    }


}