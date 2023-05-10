/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Singleton
public class AccountPropertyObserver {

    private final FundingMonitorService fundingMonitorService;

    @Inject
    public AccountPropertyObserver(FundingMonitorService fundingMonitorService) {
        this.fundingMonitorService = fundingMonitorService;
    }

    /**
     * Property event notification
     *
     * @param property Account property
     */
    public void onAccountSetProperty(@Observes @AccountEvent(AccountEventType.SET_PROPERTY) AccountProperty property) {
        log.debug("Catch event {} property={}", AccountEventType.SET_PROPERTY, property);
        if (fundingMonitorService.isStopped()) {
            return;
        }
        long accountId = property.getRecipientId();
        try {
            boolean addMonitoredAccount = true;
            synchronized (fundingMonitorService.getMonitors()) {
                //
                // Check if updating an existing monitored account.  In this case, we don't need to create
                // a new monitored account and just need to update any monitor overrides.
                //
                List<MonitoredAccount> accountList = fundingMonitorService.getMonitoredAccountListById(accountId);
                log.debug("accountList = {}", accountList);
                if (accountList != null) {
                    for (MonitoredAccount account : accountList) {
                        if (account.getMonitor().getProperty().equals(property.getProperty())) {
                            addMonitoredAccount = false;
                            MonitoredAccount newAccount = fundingMonitorService.createMonitoredAccount(
                                accountId, account.getMonitor(), property.getValue());
                            account.setAmount(newAccount.getAmount());
                            account.setThreshold( newAccount.getThreshold() );
                            account.setInterval( newAccount.getInterval() );
                            fundingMonitorService.addPendingEvent(account);
                            log.debug(
                               "Updated {} monitor for account {}, property '{}', holding {}, "
                                        + "amount {}, threshold {}, interval {}",
                                    account.getMonitor().getHoldingType().name(), account.getAccountName(),
                                    property.getProperty(), account.getMonitor().getHoldingId(),
                                    account.getAmount(), account.getThreshold(), account.getInterval());
                        }
                    }
                }
                //
                // Create a new monitored account if there is an active monitor for this account property
                //
                if (addMonitoredAccount) {
                    for (FundingMonitorInstance monitor : fundingMonitorService.getMonitors()) {
                        if (monitor.getProperty().equals(property.getProperty())) {
                            MonitoredAccount account =
                                fundingMonitorService.createMonitoredAccount(accountId, monitor, property.getValue());
                            accountList = fundingMonitorService.getMonitoredAccountListById(accountId);
                            if (accountList == null) {
                                accountList = new ArrayList<>();
                                fundingMonitorService.putAccountList(accountId, accountList);
                            }
                            accountList.add(account);
                            fundingMonitorService.addPendingEvent(account);
                            log.debug(
                                "Created {} monitor for account {}, property '{}', holding {} "
                                        + "amount {}, threshold {}, interval {}",
                                    monitor.getHoldingType().name(), account.getAccountName(),
                                    property.getProperty(), monitor.getHoldingId(),
                                    account.getAmount(), account.getThreshold(), account.getInterval());
                        }
                    }
                }
            }
        } catch (Exception exc) {
            log.error("Unable to process SET_PROPERTY event for account " + Convert2.rsAccount(accountId), exc);
        }
    }

    public void onAccountDeleteProperty(@Observes @AccountEvent(AccountEventType.DELETE_PROPERTY) AccountProperty property) {
        log.trace("Catch event {} property={}", AccountEventType.DELETE_PROPERTY, property);
        if (fundingMonitorService.isStopped()) {
            return;
        }
        long accountId = property.getRecipientId();
        synchronized (fundingMonitorService.getMonitors()) {
            List<MonitoredAccount> accountList = fundingMonitorService.getMonitoredAccountListById(accountId);
            if (accountList != null) {
                Iterator<MonitoredAccount> it = accountList.iterator();
                while (it.hasNext()) {
                    MonitoredAccount account = it.next();
                    if (account.getMonitor().getProperty().equals(property.getProperty())) {
                        it.remove();
                        log.debug(
                            "Deleted {} monitor for account {}, property '{}', holding {}",
                                account.getMonitor().getHoldingType().name(), account.getAccountName(),
                                property.getProperty(), account.getMonitor().getHoldingId());
                    }
                }
                if (accountList.isEmpty()) {
                    fundingMonitorService.removeByAccountId(accountId);
                }
            }
        }
    }
}
