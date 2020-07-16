/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.funding;

import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import com.apollocurrency.aplwallet.apl.core.app.FundingMonitor;
import com.apollocurrency.aplwallet.apl.core.app.runnable.FundingMonitorProcessEventsThread;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPropertyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class FundingMonitorServiceImpl {

   /**
    * Active monitors
    */
    private static final List<FundingMonitor> monitors = new ArrayList<>();
    /**
     * Monitored accounts
     */
    private static final Map<Long, List<MonitoredAccount>> accounts = new HashMap<>();
    /**
     * Process semaphore
     */
    private static final Semaphore processSemaphore = new Semaphore(0);
    /**
     * Pending updates
     */
    private final ConcurrentLinkedQueue<MonitoredAccount> pendingEvents = new ConcurrentLinkedQueue<>();

    private final PropertiesHolder propertiesLoader;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final FeeCalculator feeCalculator = new FeeCalculator();
    private final TransactionProcessor transactionProcessor;
    private final GlobalSync globalSync; // prevent fail on node shutdown
    private final AccountService accountService;
    private final AccountAssetService accountAssetService;
    private final AccountCurrencyService accountCurrencyService;
    private final AccountPropertyService accountPropertyService;
    /**
     * Maximum number of monitors
     */
    private static int MAX_MONITORS;// propertiesLoader.getIntProperty("apl.maxNumberOfMonitors");
    /**
     * Monitor started
     */
    private static volatile boolean started = false;
    /**
     * Monitor stopped
     */
    private static volatile boolean stopped = false;

    public FundingMonitorServiceImpl(PropertiesHolder propertiesLoader,
                                     BlockchainConfig blockchainConfig,
                                     Blockchain blockchain,
                                     TransactionProcessor transactionProcessor,
                                     GlobalSync globalSync,
                                     AccountService accountService,
                                     AccountAssetService accountAssetService,
                                     AccountCurrencyService accountCurrencyService,
                                     AccountPropertyService accountPropertyService) {
        this.propertiesLoader = Objects.requireNonNull(propertiesLoader);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.globalSync = Objects.requireNonNull(globalSync);
        this.accountService = Objects.requireNonNull(accountService);
        this.accountAssetService = Objects.requireNonNull(accountAssetService);
        this.accountCurrencyService = Objects.requireNonNull(accountCurrencyService);
        this.accountPropertyService = Objects.requireNonNull(accountPropertyService);
        /** Maximum number of monitors */
        MAX_MONITORS = propertiesLoader.getIntProperty("apl.maxNumberOfMonitors");
    }


    /**
     * Initialize monitor processing
     */
    private synchronized void init() {
        if (stopped) {
            throw new RuntimeException("Account monitor processing has been stopped");
        }
        if (started) {
            return;
        }
        try {
            //
            // Create the monitor processing thread
            //
            Runnable processingThread = new FundingMonitorProcessEventsThread(
                this, this.blockchain, this.accountService);
//            processingThread.start();
            processingThread.run();
            started = true;
            log.debug("Account monitor initialization completed");
        } catch (RuntimeException exc) {
            stopped = true;
            log.error("Account monitor initialization failed", exc);
            throw exc;
        }
    }

}
