package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAssetBalanceObserverTest {

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    @Mock
    FundingMonitorService fundingMonitorService;
    private AccountAssetBalanceObserver observer;

    @BeforeEach
    void setUp() {
        observer = new AccountAssetBalanceObserver(fundingMonitorService);
    }

    @Test
    void onAccountAssetBalance_stoppedMonitor() {
        AccountAsset asset = new AccountAsset(1L, 1L, 100L, 200L, 1);
        when(fundingMonitorService.isStopped()).thenReturn(true);

        observer.onAccountAssetBalance(asset);

        verify(fundingMonitorService).isStopped();
    }

    @Test
    void onAccountAssetBalance() {
        AccountAsset asset = new AccountAsset(1L, 1L, 100L, 200L, 1000);
        when(fundingMonitorService.getMonitors()).thenReturn(List.of());
        byte[] keySeed = Crypto.getKeySeed(new byte[]{0, 1, 2, 3});
        MonitoredAccount monAcc1 = new MonitoredAccount(1L,
            new FundingMonitorInstance(HoldingType.ASSET, 1L, "prop",
                100L, 1000L, 10, asset.getAccountId(), keySeed),
            100L, 1000L, 10
        );
        MonitoredAccount monAcc2 = new MonitoredAccount(2L,
            new FundingMonitorInstance(HoldingType.ASSET, 1L, "prop",
                100L, 1000L, 10, asset.getAccountId(), keySeed),
            100L, 1000L, 10
        );
        when(fundingMonitorService.getMonitoredAccountListById(asset.getAccountId())).thenReturn(
          List.of(
              monAcc1,
              monAcc2
          )
        );
        when(fundingMonitorService.containsPendingEvent(any(MonitoredAccount.class))).thenReturn(false).thenReturn(false);
        lenient().when(fundingMonitorService.addPendingEvent(any(MonitoredAccount.class))).thenReturn(true).thenReturn(true);

        observer.onAccountAssetBalance(asset);

        verify(fundingMonitorService).getMonitors();
        verify(fundingMonitorService, times(2)).containsPendingEvent(any(MonitoredAccount.class));
        verify(fundingMonitorService, times(2)).addPendingEvent(any(MonitoredAccount.class));
    }
}