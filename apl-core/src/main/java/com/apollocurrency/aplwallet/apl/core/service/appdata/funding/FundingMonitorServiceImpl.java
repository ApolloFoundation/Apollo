/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.funding;

import com.apollocurrency.aplwallet.apl.core.app.runnable.FundingMonitorProcessEventsThread;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionSigner;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.FundingMonitorInstance;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.funding.MonitoredAccount;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPropertyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Monitor account balances based on account properties
 * <p/>
 * APL, ASSET and CURRENCY balances can be monitored.  If a balance falls below the threshold, a transaction
 * will be submitted to transfer units from the funding account to the monitored account.  A transfer will
 * remain pending if the number of blocks since the previous transfer transaction is less than the monitor
 * interval.
 * <p/>
 * NOTICE: The current service supports the transaction V1 signing, it doesn't support multi-sig.
 * See the document signer instantiating routine in the constructor.
 */
@Slf4j
@Singleton
public class FundingMonitorServiceImpl implements FundingMonitorService {

   /**
    * Active monitors
    */
    private static final List<FundingMonitorInstance> monitors = Collections.synchronizedList(new ArrayList<>());
    /**
     * Monitored accounts
     */
    private static Map<Long, List<MonitoredAccount>> accounts = Collections.synchronizedMap(new HashMap<>());
    /**
     * Pending updates
     */
    private final ConcurrentLinkedQueue<MonitoredAccount> pendingEvents = new ConcurrentLinkedQueue<>();

    private final PropertiesHolder propertiesHolder;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final FeeCalculator feeCalculator;
    private final TransactionProcessor transactionProcessor;
    private final GlobalSync globalSync; // prevent fail on node shutdown
    private final AccountService accountService;
    private final AccountAssetService accountAssetService;
    private final AccountCurrencyService accountCurrencyService;
    private final AccountPropertyService accountPropertyService;
    private final TaskDispatchManager taskDispatchManager;
    private final TransactionBuilderFactory transactionBuilderFactory;
    private final TransactionSigner signerService;

    //TODO Use TransactionVersionValidator#getActualVersion()
    private final int transactionVersion = 1;//transaction version during the funding routine

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
                                     AccountPropertyService accountPropertyService,
                                     FeeCalculator feeCalculator,
                                     TaskDispatchManager taskDispatchManager,
                                     TransactionBuilderFactory transactionBuilderFactory,
                                     TransactionSigner signerService) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.globalSync = Objects.requireNonNull(globalSync);
        this.accountService = Objects.requireNonNull(accountService);
        this.accountAssetService = Objects.requireNonNull(accountAssetService);
        this.accountCurrencyService = Objects.requireNonNull(accountCurrencyService);
        this.accountPropertyService = Objects.requireNonNull(accountPropertyService);
        this.transactionBuilderFactory = transactionBuilderFactory;
        /** Maximum number of monitors */
        MAX_MONITORS = this.propertiesHolder.getIntProperty("apl.maxNumberOfMonitors");
        this.taskDispatchManager = taskDispatchManager;
        this.feeCalculator = feeCalculator;
        this.signerService = signerService;
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
            // Create the Funding monitor processing thread
            //
            Runnable processingThread = new FundingMonitorProcessEventsThread(
                this, this.blockchain, this.accountService);
//            processingThread.start();
            if (!propertiesHolder.isLightClient()) {
                taskDispatchManager.newBackgroundDispatcher("FundingMonitor")
                    .schedule(Task.builder()
                        .name("FundingMonitorThread")
                        .initialDelay(500)
                        .delay(1000)
                        .task(processingThread)
                        .build());
            }
//            processingThread.run();
            started = true;
            log.debug("Account Funding monitor initialization completed");
        } catch (RuntimeException exc) {
            stopped = true;
            log.error("Account Funding monitor initialization failed", exc);
            throw exc;
        }
    }

    /**
     * Stop monitor processing
     */
    @PreDestroy
    private void shutdown() {
        log.debug("Funding monitor shutdown... was started ? = '{}'", started);
        try {
            if (started && !stopped) {
                stopped = true;
//                processSemaphore.release();
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void processSemaphoreAcquire() throws InterruptedException {
//        processSemaphore.acquire();
    }

    @Override
    public boolean addPendingEvent(MonitoredAccount account) {
        return pendingEvents.add(account);
    }

    @Override
    public MonitoredAccount pollPendingEvent() {
        return pendingEvents.poll();
    }

    @Override
    public boolean addAllPendingEvents(List<MonitoredAccount> suspendedEvents) {
        return pendingEvents.addAll(suspendedEvents);
    }

    @Override
    public boolean containsPendingEvent(MonitoredAccount monitoredAccount) {
        return pendingEvents.contains(monitoredAccount);
    }

    @Override
    public boolean isPendingEventsEmpty() {
        return pendingEvents.isEmpty();
    }

    @Override
    public List<MonitoredAccount> getMonitoredAccountListById(long accountId) {
        return accounts.get(accountId);
    }

    public List<FundingMonitorInstance> getMonitors() {
        return monitors;
    }

    @Override
    public List<MonitoredAccount> putAccountList(long accountId, List<MonitoredAccount> accountList) {
        return accounts.put(accountId, accountList);
    }

    @Override
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
    @Override
    public boolean startMonitor(HoldingType holdingType, long holdingId, String property,
                                long amount, long threshold, int interval, byte[] keySeed) {
        //
        // Initialize monitor processing if it hasn't been done yet.  We do this now
        // instead of during ARS initialization so we don't start the monitor thread if it
        // won't be used.
        //
        this.init(); // lazy start
        long accountId = AccountService.getId(Crypto.getPublicKey(keySeed));
        log.debug("startMonitor accountId={}, holdingId = {} holdingType={}, property={}, threshold={}, interval={}",
            accountId, holdingId, holdingType, property, threshold, interval);
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
                        monitor.getHoldingId(),
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
        log.debug("Started Monitor for accountId={}, holdingId = {} holdingType={}, property={}, threshold={}, interval={} = OK",
            accountId, holdingId, holdingType, property, threshold, interval);
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
    @Override
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
        MonitoredAccount monitoredAccount = new MonitoredAccount(accountId, monitor, monitorAmount, monitorThreshold, monitorInterval);
        log.debug("createed MonitoredAccount = {}", monitoredAccount);
        return monitoredAccount;
    }

    /**
     * Stop all monitors
     * <p>
     * Pending fund transactions will still be processed
     *
     * @return Number of monitors stopped
     */
    @Override
    public int stopAllMonitors() {
        int stopCount;
        synchronized (monitors) {
            stopCount = monitors.size();
            monitors.clear();
            accounts.clear();
        }
        log.info("All [{}] monitor(s) stopped", stopCount);
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
    @Override
    public boolean stopMonitor(HoldingType holdingType, long holdingId, String property, long accountId) {
        log.debug("stopMonitor accountId={}, holdingId = {} holdingType={}, property={}", accountId, holdingId, holdingType, property);
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
                log.debug("{} monitor stopped for funding account {}, property '{}', holding {}",
                    holdingType.name(), monitor.getAccountName(), monitor.getProperty(), monitor.getHoldingId());
            }
        }
        log.debug("Is stopped Monitor ? '{}' accountId={}, holdingId = {} holdingType={}, property={}",
            wasStopped, accountId, holdingId, holdingType, property);
        return wasStopped;
    }

    /**
     * Get monitors satisfying the supplied filter
     *
     * @param filter Monitor filter
     * @return Monitor list
     */
    @Override
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
    @Override
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
    public List<MonitoredAccount> getMonitoredAccounts(FundingMonitorInstance monitor) {
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
    @Override
    public void processAplEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
        throws AplException {
        log.debug("processAplEvent monitoredAccount={}, targetAccount = {} fundingAccount={}",
            monitoredAccount, targetAccount, fundingAccount);
        FundingMonitorInstance monitor = monitoredAccount.getMonitor();
        if (targetAccount.getBalanceATM() < monitoredAccount.getThreshold()) {
            int timestamp = blockchain.getLastBlockTimestamp();
            Transaction.Builder builder = transactionBuilderFactory.newTransactionBuilder(transactionVersion, monitor.getPublicKey(),
                monitoredAccount.getAmount(), 0, (short) 1440, Attachment.ORDINARY_PAYMENT, timestamp)
                .recipientId(monitoredAccount.getAccountId())
                .ecBlockData(blockchain.getECBlock(timestamp));

            Transaction transaction = builder.build();
            long minimumFeeATM = feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight());
            transaction.setFeeATM(minimumFeeATM);
            if (Math.addExact(monitoredAccount.getAmount(), transaction.getFeeATM()) > fundingAccount.getUnconfirmedBalanceATM()) {
                log.warn("Funding account {} has insufficient funds; funding transaction discarded",
                    monitor.getAccountName());
            } else {
                signerService.sign(transaction, monitor.getKeySeed());
                transactionProcessor.broadcast(transaction);
                monitoredAccount.setHeight(blockchain.getHeight());
                log.debug("{} funding transaction {} for {} {} submitted from {} to {}",
                    blockchainConfig.getCoinSymbol(), transaction.getStringId(),
                    (double) monitoredAccount.getAmount() / blockchainConfig.getOneAPL(),
                    blockchainConfig.getCoinSymbol(), monitor.getAccountName(),
                    monitoredAccount.getAccountName());
            }
        } else {
            log.debug("processAplEvent - Nothing to process, targetATM={}, monitoredThreshold={},  condition = '{}'",
                targetAccount.getBalanceATM(), monitoredAccount.getThreshold(),
                targetAccount.getBalanceATM() < monitoredAccount.getThreshold());
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
    @Override
    public void processAssetEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
        throws AplException {
        log.debug("processAssetEvent monitoredAccount={}, targetAccount = {} fundingAccount={}",
            monitoredAccount, targetAccount, fundingAccount);
        FundingMonitorInstance monitor = monitoredAccount.getMonitor();
        AccountAsset targetAsset = accountAssetService.getAsset(targetAccount, monitor.getHoldingId());
        AccountAsset fundingAsset = accountAssetService.getAsset(fundingAccount, monitor.getHoldingId());
        log.debug("processAssetEvent targetAsset={}, fundingAsset = {}", targetAsset, fundingAsset);
        if (fundingAsset == null || fundingAsset.getUnconfirmedQuantityATU() < monitoredAccount.getAmount()) {
            log.warn("Funding account {} has insufficient quantity for asset {}; funding transaction discarded",
                    monitor.getAccountName(), monitor.getHoldingId());
        } else if (targetAsset == null || targetAsset.getQuantityATU() < monitoredAccount.getThreshold()) {
            Attachment attachment = new ColoredCoinsAssetTransfer(monitor.getHoldingId(), monitoredAccount.getAmount());
            int timestamp = blockchain.getLastBlockTimestamp();
            Transaction.Builder builder = transactionBuilderFactory.newTransactionBuilder(transactionVersion, monitor.getPublicKey(),
                0, 0, (short) 1440, attachment, timestamp)
                .recipientId(monitoredAccount.getAccountId())
                .ecBlockData(blockchain.getECBlock(timestamp));

            Transaction transaction = builder.build();
            transaction.setFeeATM(feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight()));
            if (transaction.getFeeATM() > fundingAccount.getUnconfirmedBalanceATM()) {
                log.warn("Funding account {} has insufficient funds; funding transaction discarded",
                    monitor.getAccountName());
            } else {
                signerService.sign(transaction, monitor.getKeySeed());
                transactionProcessor.broadcast(transaction);
                monitoredAccount.setHeight(blockchain.getHeight());
                log.debug("ASSET funding transaction {} submitted for {} units from {} to {}",
                    transaction.getStringId(), monitoredAccount.getAmount(),
                    monitor.getAccountName(), monitoredAccount.getAccountName());
            }
        } else {
            log.debug("processAssetEvent - Nothing to process...");
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
    @Override
    public void processCurrencyEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
        throws AplException {
        log.debug("processCurrencyEvent monitoredAccount={}, targetAccount = {} fundingAccount={}",
            monitoredAccount, targetAccount, fundingAccount);
        FundingMonitorInstance monitor = monitoredAccount.getMonitor();
        AccountCurrency targetCurrency = accountCurrencyService.getAccountCurrency(targetAccount.getId(), monitor.getHoldingId());
        AccountCurrency fundingCurrency = accountCurrencyService.getAccountCurrency(fundingAccount.getId(), monitor.getHoldingId());
        log.debug("processAssetEvent targetCurrency={}, fundingCurrency = {}", targetCurrency, fundingCurrency);
        if (fundingCurrency == null || fundingCurrency.getUnconfirmedUnits() < monitoredAccount.getAmount()) {
            log.warn("Funding account {} has insufficient quantity for currency {}; funding transaction discarded",
                    monitor.getAccountName(), monitor.getHoldingId());
        } else if (targetCurrency == null || targetCurrency.getUnits() < monitoredAccount.getThreshold()) {
            Attachment attachment = new MonetarySystemCurrencyTransfer(monitor.getHoldingId(), monitoredAccount.getAmount());
            int timestamp = blockchain.getLastBlockTimestamp();
            Transaction.Builder builder = transactionBuilderFactory.newTransactionBuilder(transactionVersion, monitor.getPublicKey(),
                0, 0, (short) 1440, attachment, timestamp)
                .recipientId(monitoredAccount.getAccountId())
                .ecBlockData(blockchain.getECBlock(timestamp));

            Transaction transaction = builder.build();
            transaction.setFeeATM(feeCalculator.getMinimumFeeATM(transaction, blockchain.getHeight()));
            if (transaction.getFeeATM() > fundingAccount.getUnconfirmedBalanceATM()) {
                log.warn("Funding account {} has insufficient funds; funding transaction discarded",
                    monitor.getAccountName());
            } else {
                signerService.sign(transaction, monitor.getKeySeed());
                transactionProcessor.broadcast(transaction);
                monitoredAccount.setHeight(blockchain.getHeight());
                log.debug("CURRENCY funding transaction {} submitted for {} units from {} to {}",
                    transaction.getStringId(), monitoredAccount.getAmount(),
                    monitor.getAccountName(), monitoredAccount.getAccountName());
            }
        } else {
            log.debug("processCurrencyEvent  - Nothing to process, monitoredAccount={}, targetAccount = {} fundingAccount={}",
                monitoredAccount, targetAccount, fundingAccount);
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
