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
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AccountCurrencyBalanceObserver {

    private final FundingMonitorService fundingMonitorService;

    @Inject
    public AccountCurrencyBalanceObserver(FundingMonitorService fundingMonitorService) {
        this.fundingMonitorService = fundingMonitorService;
    }

    /**
     * Currency event notification
     *
     * @param currency Account currency
     */
    public void onAccountCurrencyBalance(@Observes @AccountEvent(AccountEventType.CURRENCY_BALANCE) AccountCurrency currency) {
        log.trace("Catch event {} currency={}", AccountEventType.CURRENCY_BALANCE, currency);
        if (fundingMonitorService.isStopped()) {
            return;
        }
        long balance = currency.getUnits();
        long currencyId = currency.getCurrencyId();
        //
        // Check the currency balance for monitored accounts
        //
        synchronized (fundingMonitorService.getMonitors()) {
            List<MonitoredAccount> accountList = fundingMonitorService.getMonitoredAccountListById(currency.getAccountId());
            if (accountList != null) {
                accountList.forEach((maccount) -> {
                    if (maccount.getMonitor().getHoldingType() == HoldingType.CURRENCY &&
                        maccount.getMonitor().getHoldingId() == currencyId &&
                        balance < maccount.getThreshold() &&
                        !fundingMonitorService.containsPendingEvent(maccount)) {
                        fundingMonitorService.addPendingEvent(maccount);
                    }
                });
            }
        }
    }


}
