/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.funding;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.runnable.FundingMonitorProcessEventsThread;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPropertyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 * Monitor account balances based on account properties
 * <p>
 * APL, ASSET and CURRENCY balances can be monitored.  If a balance falls below the threshold, a transaction
 * will be submitted to transfer units from the funding account to the monitored account.  A transfer will
 * remain pending if the number of blocks since the previous transfer transaction is less than the monitor
 * interval.
 */
@Slf4j
@Vetoed
public class FundingMonitorServiceImpl {

   /**
    * Active monitors
    */
    private static final List<FundingMonitorInstance> monitors = new ArrayList<>();
    /**
     * Monitored accounts
     */
    private static Map<Long, List<MonitoredAccount>> accounts = new HashMap<>();
    /**
     * Process semaphore
     */
    private static final Semaphore processSemaphore = new Semaphore(0);
    /**
     * Pending updates
     */
    private final ConcurrentLinkedQueue<MonitoredAccount> pendingEvents = new ConcurrentLinkedQueue<>();

    private final PropertiesHolder propertiesHolder;
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
    private static int MAX_MONITORS;
    /**
     * Monitor started
     */
    private volatile boolean started = false;
    /**
     * Monitor stopped
     */
    private volatile boolean stopped = false;

    @Inject
    public FundingMonitorServiceImpl(PropertiesHolder propertiesHolder,
                                     BlockchainConfig blockchainConfig,
                                     Blockchain blockchain,
                                     TransactionProcessor transactionProcessor,
                                     GlobalSync globalSync,
                                     AccountService accountService,
                                     AccountAssetService accountAssetService,
                                     AccountCurrencyService accountCurrencyService,
                                     AccountPropertyService accountPropertyService) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.globalSync = Objects.requireNonNull(globalSync);
        this.accountService = Objects.requireNonNull(accountService);
        this.accountAssetService = Objects.requireNonNull(accountAssetService);
        this.accountCurrencyService = Objects.requireNonNull(accountCurrencyService);
        this.accountPropertyService = Objects.requireNonNull(accountPropertyService);
        /** Maximum number of monitors */
        MAX_MONITORS = this.propertiesHolder.getIntProperty("apl.maxNumberOfMonitors");
    }


    @PostConstruct
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

    public boolean isStopped() {
        return stopped;
    }

    public void processSemaphoreAcquire() throws InterruptedException {
        processSemaphore.acquire();
    }

    public boolean addPendingEvent(MonitoredAccount account) {
        return pendingEvents.add(account);
    }

    public MonitoredAccount pollPendingEvent() {
        return pendingEvents.poll();
    }

    public boolean addAllPendingEvents(List<MonitoredAccount> suspendedEvents) {
        return pendingEvents.addAll(suspendedEvents);
    }

    public boolean containsPendingEvent(MonitoredAccount monitoredAccount) {
        return pendingEvents.contains(monitoredAccount);
    }

    public boolean isPendingEventsEmpty() {
        return pendingEvents.isEmpty();
    }

    public List<MonitoredAccount> getMonitoredAccountById(long accountId) {
        return accounts.get(accountId);
    }

    public static List<FundingMonitorInstance> getMonitors() {
        return monitors;
    }

    public List<MonitoredAccount> putAccountList(long accountId, List<MonitoredAccount> accountList) {
        return accounts.put(accountId, accountList);
    }

    public List<MonitoredAccount> removeByAccountId(long accountId) {
        return accounts.remove(accountId);
    }

    /**
     * Start the monitor
     * <p>
     * One or more funding parameters can be overridden in the account property value
     * string: {"amount":"long","threshold":"long","interval":integer}
     *
     * @param holdingType Holding type
     * @param holdingId   Asset or currency identifier, ignored for APL monitor
     * @param property    Account property name
     * @param amount      Fund amount
     * @param threshold   Fund threshold
     * @param interval    Fund interval
     * @param keySeed     Fund account keySeed
     * @return TRUE if the monitor was started
     */
    public boolean startMonitor(HoldingType holdingType, long holdingId, String property,
                                       long amount, long threshold, int interval, byte[] keySeed) {
        //
        // Initialize monitor processing if it hasn't been done yet.  We do this now
        // instead of during ARS initialization so we don't start the monitor thread if it
        // won't be used.
        //
        long accountId = AccountService.getId(Crypto.getPublicKey(keySeed));
        //
        // Create the monitor
        //
        FundingMonitorInstance monitor = new FundingMonitorInstance(holdingType, holdingId, property,
            amount, threshold, interval, accountId, keySeed);
        globalSync.readLock();
        try {
            //
            // Locate monitored accounts based on the account property and the setter identifier
            //
            List<MonitoredAccount> accountList = new ArrayList<>();
            List<AccountProperty> properties = accountPropertyService.getProperties(0, accountId, property,
                0, Integer.MAX_VALUE);
            properties.forEach(accountProperty -> {
                MonitoredAccount account = createMonitoredAccount(accountProperty.getRecipientId(),
                    monitor, accountProperty.getValue());
                accountList.add(account);
            });
            //
            // Activate the monitor and check each monitored account to see if we need to submit
            // an initial fund transaction
            //
            synchronized (monitors) {
                if (monitors.size() > MAX_MONITORS) {
                    throw new RuntimeException("Maximum of " + MAX_MONITORS + " monitors already started");
                }
                if (monitors.contains(monitor)) {
                    log.debug("{} monitor already started for account {}, property '{}', holding {}",
                        holdingType.name(), monitor.getAccountName(), property, Long.toUnsignedString(holdingId));
                    return false;
                }
                accountList.forEach(account -> {
                    List<MonitoredAccount> activeList = accounts.get(account.getAccountId());
                    if (activeList == null) {
                        activeList = new ArrayList<>();
                        accounts.put(account.getAccountId(), activeList);
                    }
                    activeList.add(account);
                    pendingEvents.add(account);
                    log.debug("Created {} monitor for target account {}, property '{}', holding {}, "
                            + "amount {}, threshold {}, interval {}",
                        holdingType.name(), account.getAccountName(), monitor.getProperty(),
                        Long.toUnsignedString(monitor.getHoldingId()),
                        account.getAmount(), account.getThreshold(), account.getInterval());
                });
                monitors.add(monitor);
                log.info("{} monitor started for funding account {}, property '{}', holding {}",
                    holdingType.name(), monitor.getAccountName(), monitor.getProperty(),
                    Long.toUnsignedString(monitor.getHoldingId()));
            }
        } finally {
            globalSync.readUnlock();
        }
        return true;
    }


    /**
     * Create a monitored account
     * <p>
     * The amount, threshold and interval values specified when the monitor was started can be overridden
     * by specifying one or more values in the property value string
     *
     * @param accountId     Account identifier
     * @param monitor       Account monitor
     * @param propertyValue Account property value
     * @return Monitored account
     */
    public MonitoredAccount createMonitoredAccount(long accountId, FundingMonitorInstance monitor, String propertyValue) {
        long monitorAmount = monitor.getAmount();
        long monitorThreshold = monitor.getThreshold();
        int monitorInterval = monitor.getInterval();
        if (propertyValue != null && !propertyValue.isEmpty()) {
            try {
                Object parsedValue = JSONValue.parseWithException(propertyValue);
                if (!(parsedValue instanceof JSONObject)) {
                    throw new IllegalArgumentException("Property value is not a JSON object");
                }
                JSONObject jsonValue = (JSONObject) parsedValue;
                monitorAmount = getValue(jsonValue.get("amount"), monitorAmount);
                monitorThreshold = getValue(jsonValue.get("threshold"), monitorThreshold);
                monitorInterval = (int) getValue(jsonValue.get("interval"), monitorInterval);
            } catch (IllegalArgumentException | ParseException exc) {
                String errorMessage = String.format("Account %s, property '%s', value '%s' is not valid",
                    Convert2.rsAccount(accountId), monitor.getProperty(), propertyValue);
                throw new IllegalArgumentException(errorMessage, exc);
            }
        }
        return new MonitoredAccount(accountId, monitor, monitorAmount, monitorThreshold, monitorInterval);
    }

    /**
     * Stop all monitors
     * <p>
     * Pending fund transactions will still be processed
     *
     * @return Number of monitors stopped
     */
    public int stopAllMonitors() {
        int stopCount;
        synchronized (monitors) {
            stopCount = monitors.size();
            monitors.clear();
            accounts.clear();
        }
        log.info("All monitors stopped");
        return stopCount;
    }


    /**
     * Stop monitor
     * <p>
     * Pending fund transactions will still be processed
     *
     * @param holdingType Monitor holding type
     * @param holdingId   Asset or currency identifier, ignored for APL monitor
     * @param property    Account property
     * @param accountId   Fund account identifier
     * @return TRUE if the monitor was stopped
     */
    public boolean stopMonitor(HoldingType holdingType, long holdingId, String property, long accountId) {
        FundingMonitorInstance monitor = null;
        boolean wasStopped = false;
        synchronized (monitors) {
            //
            // Deactivate the monitor
            //
            Iterator<FundingMonitorInstance> monitorIt = monitors.iterator();
            while (monitorIt.hasNext()) {
                monitor = monitorIt.next();
                if (monitor.getHoldingType() == holdingType && monitor.getProperty().equals(property) &&
                    (holdingType == HoldingType.APL || monitor.getHoldingId() == holdingId) &&
                    monitor.getAccountId() == accountId) {
                    monitorIt.remove();
                    wasStopped = true;
                    break;
                }
            }
            //
            // Remove monitored accounts (pending fund transactions will still be processed)
            //
            if (wasStopped) {
                Iterator<List<MonitoredAccount>> accountListIt = accounts.values().iterator();
                while (accountListIt.hasNext()) {
                    List<MonitoredAccount> accountList = accountListIt.next();
                    Iterator<MonitoredAccount> accountIt = accountList.iterator();
                    while (accountIt.hasNext()) {
                        MonitoredAccount account = accountIt.next();
                        if (account.getMonitor() == monitor) {
                            accountIt.remove();
                            if (accountList.isEmpty()) {
                                accountListIt.remove();
                            }
                            break;
                        }
                    }
                }
                log.info("{} monitor stopped for fund account {}, property '{}', holding {}",
                    holdingType.name(), monitor.getAccountName(), monitor.getProperty(), monitor.getHoldingId());
            }
        }
        return wasStopped;
    }

    /**
     * Get monitors satisfying the supplied filter
     *
     * @param filter Monitor filter
     * @return Monitor list
     */
    public List<FundingMonitorInstance> getMonitors(Filter<FundingMonitorInstance> filter) {
        List<FundingMonitorInstance> result = new ArrayList<>();
        synchronized (monitors) {
            monitors.forEach((monitor) -> {
                if (filter.test(monitor)) {
                    result.add(monitor);
                }
            });
        }
        return result;
    }

    /**
     * Get all monitors
     *
     * @return Account monitor list
     */
    public List<FundingMonitorInstance> getAllMonitors() {
        List<FundingMonitorInstance> allMonitors = new ArrayList<>();
        synchronized (monitors) {
            allMonitors.addAll(monitors);
        }
        return allMonitors;
    }

    /**
     * Get all monitored accounts for a single monitor
     *
     * @param monitor Monitor
     * @return List of monitored accounts
     */
    public static List<MonitoredAccount> getMonitoredAccounts(FundingMonitorInstance monitor) {
        List<MonitoredAccount> monitoredAccounts = new ArrayList<>();
        synchronized (monitors) {
            accounts.values().forEach(monitorList -> monitorList.forEach(account -> {
                if (account.getMonitor().equals(monitor)) {
                    monitoredAccounts.add(account);
                }
            }));
        }
        return monitoredAccounts;
    }

    /**
     * Process a APL event
     *
     * @param monitoredAccount Monitored account
     * @param targetAccount    Target account
     * @param fundingAccount   Funding account
     * @throws AplException Unable to create transaction
     */
    public void processAplEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
        throws AplException {
        FundingMonitorInstance monitor = monitoredAccount.getMonitor();
        if (targetAccount.getBalanceATM() < monitoredAccount.getThreshold()) {
            Transaction.Builder builder = Transaction.newTransactionBuilder(monitor.getPublicKey(),
                monitoredAccount.getAmount(), 0, (short) 1440,
                Attachment.ORDINARY_PAYMENT, blockchain.getLastBlockTimestamp());

            builder.recipientId(monitoredAccount.getAccountId());
            Transaction transaction = builder.build(null);
            long minimumFeeATM = feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight());
            transaction.setFeeATM(minimumFeeATM);
            transaction.sign(monitor.getKeySeed());
            if (Math.addExact(monitoredAccount.getAmount(), transaction.getFeeATM()) > fundingAccount.getUnconfirmedBalanceATM()) {
                log.warn("Funding account {} has insufficient funds; funding transaction discarded",
                    monitor.getAccountName());
            } else {
                transactionProcessor.broadcast(transaction);
                monitoredAccount.setHeight( blockchain.getHeight() );
                log.debug("{} funding transaction {} for {} {} submitted from {} to {}",
                    blockchainConfig.getCoinSymbol(), transaction.getStringId(),
                    (double) monitoredAccount.getAmount() / Constants.ONE_APL,
                    blockchainConfig.getCoinSymbol(), monitor.getAccountName(),
                    monitoredAccount.getAccountName());
            }
        }
    }


    /**
     * Process an ASSET event
     *
     * @param monitoredAccount Monitored account
     * @param targetAccount    Target account
     * @param fundingAccount   Funding account
     * @throws AplException Unable to create transaction
     */
    public void processAssetEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
        throws AplException {
        FundingMonitorInstance monitor = monitoredAccount.getMonitor();
        AccountAsset targetAsset = accountAssetService.getAsset(targetAccount, monitor.getHoldingId());
        AccountAsset fundingAsset = accountAssetService.getAsset(fundingAccount, monitor.getHoldingId());
        if (fundingAsset == null || fundingAsset.getUnconfirmedQuantityATU() < monitoredAccount.getAmount()) {
            log.warn("Funding account {} has insufficient quantity for asset {}; funding transaction discarded",
                    monitor.getAccountName(), monitor.getHoldingId());
        } else if (targetAsset == null || targetAsset.getQuantityATU() < monitoredAccount.getThreshold()) {
            Attachment attachment = new ColoredCoinsAssetTransfer(monitor.getHoldingId(), monitoredAccount.getAmount());
            Transaction.Builder builder = Transaction.newTransactionBuilder(monitor.getPublicKey(),
                0, 0, (short) 1440, attachment, blockchain.getLastBlockTimestamp());
            builder.recipientId(monitoredAccount.getAccountId());
            Transaction transaction = builder.build(null);
            transaction.setFeeATM(feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight()));
            transaction.sign(monitor.getKeySeed());
            if (transaction.getFeeATM() > fundingAccount.getUnconfirmedBalanceATM()) {
                log.warn("Funding account {} has insufficient funds; funding transaction discarded",
                    monitor.getAccountName());
            } else {
                transactionProcessor.broadcast(transaction);
                monitoredAccount.setHeight( blockchain.getHeight() );
                log.debug("ASSET funding transaction {} submitted for {} units from {} to {}",
                    transaction.getStringId(), monitoredAccount.getAmount(),
                    monitor.getAccountName(), monitoredAccount.getAccountName());
            }
        }
    }

    /**
     * Process a CURRENCY event
     *
     * @param monitoredAccount Monitored account
     * @param targetAccount    Target account
     * @param fundingAccount   Funding account
     * @throws AplException Unable to create transaction
     */
    public void processCurrencyEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
        throws AplException {
        FundingMonitorInstance monitor = monitoredAccount.getMonitor();
        AccountCurrency targetCurrency = accountCurrencyService.getAccountCurrency(targetAccount.getId(), monitor.getHoldingId());
        AccountCurrency fundingCurrency = accountCurrencyService.getAccountCurrency(fundingAccount.getId(), monitor.getHoldingId());
        if (fundingCurrency == null || fundingCurrency.getUnconfirmedUnits() < monitoredAccount.getAmount()) {
            log.warn("Funding account {} has insufficient quantity for currency {}; funding transaction discarded",
                    monitor.getAccountName(), monitor.getHoldingId());
        } else if (targetCurrency == null || targetCurrency.getUnits() < monitoredAccount.getThreshold()) {
            Attachment attachment = new MonetarySystemCurrencyTransfer(monitor.getHoldingId(), monitoredAccount.getAmount());
            Transaction.Builder builder = Transaction.newTransactionBuilder(monitor.getPublicKey(),
                0, 0, (short) 1440, attachment, blockchain.getLastBlockTimestamp());
            builder.recipientId(monitoredAccount.getAccountId());
            Transaction transaction = builder.build(null);
            transaction.setFeeATM(feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight()));
            transaction.sign(monitor.getKeySeed());
            if (transaction.getFeeATM() > fundingAccount.getUnconfirmedBalanceATM()) {
                log.warn("Funding account {} has insufficient funds; funding transaction discarded",
                    monitor.getAccountName());
            } else {
                transactionProcessor.broadcast(transaction);
                monitoredAccount.setHeight( blockchain.getHeight() );
                log.debug("CURRENCY funding transaction {} submitted for {} units from {} to {}",
                    transaction.getStringId(), monitoredAccount.getAmount(),
                    monitor.getAccountName(), monitoredAccount.getAccountName());
            }
        }
    }

    /**
     * Convert a JSON parameter to a numeric value
     *
     * @param jsonValue    The parsed JSON value
     * @param defaultValue The default value
     * @return The JSON value or the default value
     */
    private static long getValue(Object jsonValue, long defaultValue) {
        if (jsonValue == null) {
            return defaultValue;
        }
        return Convert.parseLong(jsonValue);
    }

}
