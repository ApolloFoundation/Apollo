/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.appdata.funding.FundingMonitorService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import lombok.extern.slf4j.Slf4j;

/**
 * Process pending account event
 */
@Slf4j
public class FundingMonitorProcessEventsThread implements Runnable {

    private final FundingMonitorService fundingMonitorService;
    private final Blockchain blockchain;
    private final AccountService accountService;

    public FundingMonitorProcessEventsThread(FundingMonitorService fundingMonitorService,
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
        log.debug("Account Funding monitor thread started");
        List<MonitoredAccount> suspendedEvents = new ArrayList<>();
        try {
            while (true) {
                //
                // Wait for a block to be pushed and then process pending account events
                //
//                fundingMonitorService.processSemaphoreAcquire();
                if (fundingMonitorService.isStopped()) {
                    log.debug("Account Funding monitor thread stopped");
                    break;
                }
                MonitoredAccount monitoredAccount;
                while (true) {
                    boolean takeSomethingToProcess = (monitoredAccount = fundingMonitorService.pollPendingEvent()) != null;
                    log.debug("If there is somethingToProcess = ? {}", takeSomethingToProcess);
                    if (!takeSomethingToProcess) {
                        log.debug("Nothing to process in Account Funding monitor thread...");
                        break;
                    }
                    try {
                        log.debug("Let's try to process something in Account Funding monitor thread");
                        Account targetAccount = accountService.getAccount(monitoredAccount.getAccountId());
                        Account fundingAccount = accountService.getAccount(monitoredAccount.getMonitor().getAccountId());
                        log.debug("Let's process targetAccount={}, fundingAccount={}", targetAccount, fundingAccount);
                        if (blockchain.getHeight() - monitoredAccount.getHeight() < monitoredAccount.getInterval()) {
                            if (!suspendedEvents.contains(monitoredAccount)) {
                                log.debug("added monitoredAccount={}", monitoredAccount);
                                suspendedEvents.add(monitoredAccount);
                            }
                        } else if (targetAccount == null) {
                            log.error("Monitored account {} no longer exists", monitoredAccount.getAccountName());
                        } else if (fundingAccount == null) {
                            log.error("Funding account {} no longer exists", monitoredAccount.getMonitor().getAccountName());
                        } else {
                            switch (monitoredAccount.getMonitor().getHoldingType()) {
                                case APL:
                                    log.debug("processAplEvent for '{}' targetAccount={}, fundingAccount={}",
                                        monitoredAccount.getMonitor().getAccountName(), targetAccount, fundingAccount);
                                    fundingMonitorService.processAplEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                                case ASSET:
                                    log.debug("processAssetEvent for '{}' targetAccount={}, fundingAccount={}",
                                        monitoredAccount.getMonitor().getAccountName(), targetAccount, fundingAccount);
                                    fundingMonitorService.processAssetEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                                case CURRENCY:
                                    log.debug("processCurrencyEvent for '{}' targetAccount={}, fundingAccount={}",
                                        monitoredAccount.getMonitor().getAccountName(), targetAccount, fundingAccount);
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
                    log.debug("add all suspendedEvents = [{}]", suspendedEvents.size());
                    fundingMonitorService.addAllPendingEvents(suspendedEvents);
                    suspendedEvents.clear();
                }
            }
//        } catch (InterruptedException exc) {
//            log.debug("Account monitor thread interrupted");
        } catch (Throwable exc) {
            log.error("Account Funding monitor thread terminated", exc);
        }
    }


}
