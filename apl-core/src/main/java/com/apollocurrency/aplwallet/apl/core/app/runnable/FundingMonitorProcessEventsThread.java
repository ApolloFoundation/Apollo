/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FundingMonitorProcessEventsThread implements Runnable {

    private final FundingMonitorServiceImpl fundingMonitorService;
    private final Blockchain blockchain;
    private final AccountService accountService;

    public FundingMonitorProcessEventsThread(FundingMonitorServiceImpl fundingMonitorService,
                                             Blockchain blockchain,
                                             AccountService accountService) {
        this.fundingMonitorService = fundingMonitorService;
        this.blockchain = Objects.requireNonNull(blockchain);
        this.accountService = Objects.requireNonNull(accountService);
    }

    /**
     * Process pending updates
     */
    @Override
    public void run() {
        log.debug("Account monitor thread started");
        List<MonitoredAccount> suspendedEvents = new ArrayList<>();
        try {
            while (true) {
                //
                // Wait for a block to be pushed and then process pending account events
                //
                fundingMonitorService.processSemaphoreAcquire();
                if (fundingMonitorService.isStopped()) {
                    log.debug("Account monitor thread stopped");
                    break;
                }
                MonitoredAccount monitoredAccount;
                while ((monitoredAccount = fundingMonitorService.pollPendingEvent()) != null) {
                    try {
                        Account targetAccount = accountService.getAccount(monitoredAccount.getAccountId());
                        Account fundingAccount = accountService.getAccount(monitoredAccount.getMonitor().getAccountId());
                        if (blockchain.getHeight() - monitoredAccount.getHeight() < monitoredAccount.getInterval()) {
                            if (!suspendedEvents.contains(monitoredAccount)) {
                                suspendedEvents.add(monitoredAccount);
                            }
                        } else if (targetAccount == null) {
                            log.error("Monitored account {} no longer exists", monitoredAccount.getAccountName());
                        } else if (fundingAccount == null) {
                            log.error("Funding account {} no longer exists", monitoredAccount.getMonitor().getAccountName());
                        } else {
                            switch (monitoredAccount.getMonitor().getHoldingType()) {
                                case APL:
                                    fundingMonitorService.processAplEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                                case ASSET:
                                    fundingMonitorService.processAssetEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                                case CURRENCY:
                                    fundingMonitorService.processCurrencyEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                            }
                        }
                    } catch (Exception exc) {
                        log.error("Unable to process {} event for account {}, property '{}', holding {}",
                            monitoredAccount.getMonitor().getHoldingType().name(), monitoredAccount.getAccountName(),
                            monitoredAccount.getMonitor().getProperty(),
                            monitoredAccount.getMonitor().getHoldingId(), exc);
                    }
                }
                if (!suspendedEvents.isEmpty()) {
                    fundingMonitorService.addAllPendingEvents(suspendedEvents);
                    suspendedEvents.clear();
                }
            }
        } catch (InterruptedException exc) {
            log.debug("Account monitor thread interrupted");
        } catch (Throwable exc) {
            log.error("Account monitor thread terminated", exc);
        }
    }


}
