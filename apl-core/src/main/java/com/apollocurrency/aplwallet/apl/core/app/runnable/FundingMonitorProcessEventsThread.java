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
//public class FundingMonitorProcessEventsThread extends Thread {
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
                processSemaphore.acquire();
                if (stopped) {
                    log.debug("Account monitor thread stopped");
                    break;
                }
                MonitoredAccount monitoredAccount;
                while ((monitoredAccount = pendingEvents.poll()) != null) {
                    try {
                        Account targetAccount = accountService.getAccount(monitoredAccount.accountId);
                        Account fundingAccount = accountService.getAccount(monitoredAccount.monitor.accountId);
                        if (blockchain.getHeight() - monitoredAccount.height < monitoredAccount.interval) {
                            if (!suspendedEvents.contains(monitoredAccount)) {
                                suspendedEvents.add(monitoredAccount);
                            }
                        } else if (targetAccount == null) {
                            log.error(String.format("Monitored account %s no longer exists",
                                monitoredAccount.accountName));
                        } else if (fundingAccount == null) {
                            log.error(String.format("Funding account %s no longer exists",
                                monitoredAccount.monitor.accountName));
                        } else {
                            switch (monitoredAccount.monitor.holdingType) {
                                case APL:
                                    processAplEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                                case ASSET:
                                    processAssetEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                                case CURRENCY:
                                    processCurrencyEvent(monitoredAccount, targetAccount, fundingAccount);
                                    break;
                            }
                        }
                    } catch (Exception exc) {
                        log.error(String.format("Unable to process %s event for account %s, property '%s', holding %s",
                            monitoredAccount.monitor.holdingType.name(), monitoredAccount.accountName,
                            monitoredAccount.monitor.property, Long.toUnsignedString(monitoredAccount.monitor.holdingId)), exc);
                    }
                }
                if (!suspendedEvents.isEmpty()) {
                    pendingEvents.addAll(suspendedEvents);
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
