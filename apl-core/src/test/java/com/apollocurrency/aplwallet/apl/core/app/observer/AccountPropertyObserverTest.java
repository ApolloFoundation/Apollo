package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPropertyObserverTest {
    static {
        Convert2.init("APL", 1739068987193023818L);
    }
    @Mock
    FundingMonitorService fundingMonitorService;
    private AccountPropertyObserver observer;

    @BeforeEach
    void setUp() {
        this.observer = new AccountPropertyObserver(this.fundingMonitorService);
    }

    @Test
    void onAccountSetProperty_Stopped() {
        AccountProperty property = mock(AccountProperty.class);
        when(fundingMonitorService.isStopped()).thenReturn(true);

        observer.onAccountSetProperty(property);

        verify(fundingMonitorService).isStopped();
    }

    @Test
    void onAccountSetProperty() {
        AccountProperty property = new AccountProperty(
            1L, 1L, 1L, "prop", "val", 5000);
        when(fundingMonitorService.isStopped()).thenReturn(false);
        when(fundingMonitorService.getMonitors()).thenReturn(List.of());

        byte[] keySeed = Crypto.getKeySeed(new byte[]{0, 1, 2, 3});
        MonitoredAccount monAcc1 = new MonitoredAccount(1L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop",
                100L, 1000L, 10, property.getRecipientId(), keySeed),
            100L, 1000L, 10
        );
        MonitoredAccount monAcc2 = new MonitoredAccount(2L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop2",
                100L, 1000L, 10, property.getRecipientId(), keySeed),
            100L, 1000L, 10
        );
        when(fundingMonitorService.getMonitoredAccountListById(property.getRecipientId())).thenReturn(
            List.of(
                monAcc1,
                monAcc2
            )
        );
        when(fundingMonitorService.createMonitoredAccount(anyLong(), any(FundingMonitorInstance.class), anyString()))
            .thenReturn(monAcc1).thenReturn(monAcc2);

        observer.onAccountSetProperty(property);

        verify(fundingMonitorService).isStopped();
        verify(fundingMonitorService, times(1)).getMonitoredAccountListById(anyLong());
        verify(fundingMonitorService, times(1)).createMonitoredAccount(anyLong(), any(FundingMonitorInstance.class), anyString());
        verify(fundingMonitorService, times(1)).addPendingEvent(any(MonitoredAccount.class));
    }

    @Test
    void onAccountSetProperty_addAccount() {
        AccountProperty property = new AccountProperty(
            1L, 1L, 1L, "prop", "val", 5000);
        when(fundingMonitorService.isStopped()).thenReturn(false);

        byte[] keySeed = Crypto.getKeySeed(new byte[]{0, 1, 2, 3});
        MonitoredAccount monAcc1 = new MonitoredAccount(1L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop",
                100L, 1000L, 10, 1L, keySeed),
            100L, 1000L, 10
        );
        MonitoredAccount monAcc2 = new MonitoredAccount(2L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop2",
                100L, 1000L, 10, 1L, keySeed),
            100L, 1000L, 10
        );
        when(fundingMonitorService.getMonitors()).thenReturn(List.of()).thenReturn(List.of(
            new FundingMonitorInstance(HoldingType.ASSET, 1L, "prop",
                100L, 100L, 10, 1L, keySeed),
            new FundingMonitorInstance(HoldingType.ASSET, 1L, "prop",
                100L, 100L, 10, 2L, keySeed)
        ));

        when(fundingMonitorService.getMonitoredAccountListById(property.getRecipientId()))
            .thenReturn(null).thenReturn(null).thenReturn(new ArrayList<>(1));
        when(fundingMonitorService.createMonitoredAccount(anyLong(), any(FundingMonitorInstance.class), anyString()))
            .thenReturn(monAcc1).thenReturn(monAcc2);


        observer.onAccountSetProperty(property);

        verify(fundingMonitorService).isStopped();
        verify(fundingMonitorService, times(2)).getMonitors();
        verify(fundingMonitorService, times(3)).getMonitoredAccountListById(anyLong());
        verify(fundingMonitorService, times(2)).createMonitoredAccount(anyLong(), any(FundingMonitorInstance.class), anyString());
        verify(fundingMonitorService, times(2)).addPendingEvent(any(MonitoredAccount.class));
        verify(fundingMonitorService, times(1)).putAccountList(anyLong(), any(ArrayList.class));
    }

    @Test
    void onAccountSetProperty_Exception() {
        AccountProperty property = mock(AccountProperty.class);
        when(fundingMonitorService.isStopped()).thenReturn(false);
        when(fundingMonitorService.getMonitors()).thenThrow(new RuntimeException());

        observer.onAccountSetProperty(property);

        verify(fundingMonitorService).isStopped();
        verify(fundingMonitorService).getMonitors();
    }

    @Test
    void onAccountDeleteProperty_stopped() {
        AccountProperty property = mock(AccountProperty.class);
        when(fundingMonitorService.isStopped()).thenReturn(true);

        observer.onAccountSetProperty(property);

        verify(fundingMonitorService).isStopped();
    }

    @Test
    void onAccountDeleteProperty_one() {
        AccountProperty property = new AccountProperty(
            1L, 1L, 1L, "prop", "val", 5000);
        when(fundingMonitorService.isStopped()).thenReturn(false);
        when(fundingMonitorService.getMonitors()).thenReturn(List.of());

        byte[] keySeed = Crypto.getKeySeed(new byte[]{0, 1, 2, 3});
        MonitoredAccount monAcc1 = new MonitoredAccount(1L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop",
                100L, 1000L, 10, 1L, keySeed),
            100L, 1000L, 10
        );
        MonitoredAccount monAcc2 = new MonitoredAccount(2L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop2",
                100L, 1000L, 10, 1L, keySeed),
            100L, 1000L, 10
        );

        when(fundingMonitorService.getMonitoredAccountListById(property.getRecipientId()))
            .thenReturn(List.of());

        observer.onAccountDeleteProperty(property);

        verify(fundingMonitorService).isStopped();
        verify(fundingMonitorService, times(1)).getMonitors();
        verify(fundingMonitorService, times(1)).getMonitoredAccountListById(anyLong());
        verify(fundingMonitorService, times(1)).removeByAccountId(anyLong());
    }

    @Test
    void onAccountDeleteProperty_two() {
        AccountProperty property = new AccountProperty(
            1L, 1L, 1L, "prop", "val", 5000);
        when(fundingMonitorService.isStopped()).thenReturn(false);
        when(fundingMonitorService.getMonitors()).thenReturn(List.of());

        byte[] keySeed = Crypto.getKeySeed(new byte[]{0, 1, 2, 3});
        MonitoredAccount monAcc1 = new MonitoredAccount(1L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop",
                100L, 1000L, 10, 1L, keySeed),
            100L, 1000L, 10
        );
        MonitoredAccount monAcc2 = new MonitoredAccount(2L,
            new FundingMonitorInstance(HoldingType.CURRENCY, 1L, "prop2",
                100L, 1000L, 10, 1L, keySeed),
            100L, 1000L, 10
        );
        List accList = new ArrayList(2);
        accList.add(monAcc1);
        accList.add(monAcc2);
        when(fundingMonitorService.getMonitoredAccountListById(property.getRecipientId()))
            .thenReturn(accList);

        observer.onAccountDeleteProperty(property);

        verify(fundingMonitorService).isStopped();
        verify(fundingMonitorService, times(1)).getMonitors();
        verify(fundingMonitorService, times(1)).getMonitoredAccountListById(anyLong());
    }

}