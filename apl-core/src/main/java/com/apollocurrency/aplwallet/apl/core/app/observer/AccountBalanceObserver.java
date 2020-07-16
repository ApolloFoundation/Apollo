/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AccountBalanceObserver {

    private final FundingMonitorServiceImpl fundingMonitorService;

    @Inject
    public AccountBalanceObserver(FundingMonitorServiceImpl fundingMonitorService) {
        this.fundingMonitorService = fundingMonitorService;
    }

    /**
     * Account event handler (BALANCE event)
     */
    public void onAccountBalance(@Observes @AccountEvent(AccountEventType.BALANCE) Account account) {
        log.trace("Catch event {} account={}", AccountEventType.BALANCE, account);
        if (fundingMonitorService.isStopped()) {
            return;
        }
        long balance = account.getBalanceATM();
        //
        // Check the APL balance for monitored accounts
        //
        synchronized (FundingMonitorServiceImpl.getMonitors()) {
            List<MonitoredAccount> accountList = fundingMonitorService.getMonitoredAccountById(account.getId());
            if (accountList != null) {
                accountList.forEach((maccount) -> {
                    if (maccount.getMonitor().getHoldingType() == HoldingType.APL && balance < maccount.getThreshold() &&
                        !fundingMonitorService.containsPendingEvent(maccount)) {
                        fundingMonitorService.addPendingEvent(maccount);
                    }
                });
            }
        }

    }
}
