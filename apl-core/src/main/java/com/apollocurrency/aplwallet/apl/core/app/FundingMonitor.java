/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.core.account.AccountPropertyTable;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

/**
 * Monitor account balances based on account properties
 * <p>
 * APL, ASSET and CURRENCY balances can be monitored.  If a balance falls below the threshold, a transaction
 * will be submitted to transfer units from the funding account to the monitored account.  A transfer will
 * remain pending if the number of blocks since the previous transfer transaction is less than the monitor
 * interval.
 */
public class FundingMonitor {
    private static final Logger LOG = getLogger(FundingMonitor.class);


    /** Minimum monitor amount */
    public static final long MIN_FUND_AMOUNT = 1;

    /** Minimum monitor threshold */
    public static final long MIN_FUND_THRESHOLD = 1;

    /** Minimum funding interval */
    public static final int MIN_FUND_INTERVAL = 10;
    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesLoader;
    private static BlockchainConfig blockchainConfig;
    private static Blockchain blockchain;
    private static TransactionProcessor transactionProcessor;
    private static GlobalSync globalSync = CDI.current().select(GlobalSync.class).get();
    /** Maximum number of monitors */
    private static int MAX_MONITORS;// propertiesLoader.getIntProperty("apl.maxNumberOfMonitors");

    /** Monitor started */
    private static volatile boolean started = false;

    /** Monitor stopped */
    private static volatile boolean stopped = false;

    /** Active monitors */
    private static final List<FundingMonitor> monitors = new ArrayList<>();

    /** Monitored accounts */
    private static final Map<Long, List<MonitoredAccount>> accounts = new HashMap<>();

    /** Process semaphore */
    private static final Semaphore processSemaphore = new Semaphore(0);

    /** Pending updates */
    private static final ConcurrentLinkedQueue<MonitoredAccount> pendingEvents = new ConcurrentLinkedQueue<>();

    /** Account monitor holding type */
    private final HoldingType holdingType;

    /** Holding identifier */
    private final long holdingId;

    /** Account property */
    private final String property;

    /** Fund amount */
    private final long amount;

    /** Fund threshold */
    private final long threshold;

    /** Fund interval */
    private final int interval;

    /** Fund account identifier */
    private final long accountId;

    /** Fund account name */
    private final String accountName;

    /** Fund account secret phrase */
    private final byte[] keySeed;

    /** Fund account public key */
    private final byte[] publicKey;

    /**
     * Create a monitor
     *
     * @param   holdingType         Holding type
     * @param   holdingId           Asset or Currency identifier, ignored for APL monitor
     * @param   property            Account property name
     * @param   amount              Fund amount
     * @param   threshold           Fund threshold
     * @param   interval            Fund interval
     * @param   accountId           Fund account identifier
     * @param   keySeed             Fund account key seed
     */
    private FundingMonitor(HoldingType holdingType, long holdingId, String property,
                                    long amount, long threshold, int interval,
                                    long accountId, byte[] keySeed) {
        this.holdingType = holdingType;
        this.holdingId = (holdingType != HoldingType.APL ? holdingId : 0);
        this.property = property;
        this.amount = amount;
        this.threshold = threshold;
        this.interval = interval;
        this.accountId = accountId;
        this.accountName = Convert2.rsAccount(accountId);
        this.keySeed = keySeed;
        this.publicKey = Crypto.getPublicKey(keySeed);
    }

    private FundingMonitor(HoldingType holdingType, long holdingId, String property, long amount, long threshold, int interval, long accountId,
                           String accountName, byte[] keySeed, byte[] publicKey) {
        this.holdingType = holdingType;
        this.holdingId = holdingId;
        this.property = property;
        this.amount = amount;
        this.threshold = threshold;
        this.interval = interval;
        this.accountId = accountId;
        this.accountName = accountName;
        this.keySeed = keySeed;
        this.publicKey = publicKey;
    }

    /**
     * Return the monitor holding type
     *
     * @return                      Holding type
     */
    public HoldingType getHoldingType() {
        return holdingType;
    }

    /**
     * Return the holding identifier
     *
     * @return                      Holding identifier for asset or currency
     */
    public long getHoldingId() {
        return holdingId;
    }

    /**
     * Return the account property name
     *
     * @return                      Account property
     */
    public String getProperty() {
        return property;
    }

    /**
     * Return the fund amount
     *
     * @return                      Fund amount
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Return the fund threshold
     *
     * @return                      Fund threshold
     */
    public long getThreshold() {
        return threshold;
    }

    /**
     * Return the fund interval
     *
     * @return                      Fund interval
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Return the fund account identifier
     *
     * @return                      Account identifier
     */
    public long getAccountId() {
        return accountId;
    }

    /**
     * Return the fund account name
     *
     * @return                      Account name
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Start the monitor
     * <p>
     * One or more funding parameters can be overridden in the account property value
     * string: {"amount":"long","threshold":"long","interval":integer}
     *
     * @param   holdingType         Holding type
     * @param   holdingId           Asset or currency identifier, ignored for APL monitor
     * @param   property            Account property name
     * @param   amount              Fund amount
     * @param   threshold           Fund threshold
     * @param   interval            Fund interval
     * @param   keySeed             Fund account keySeed
     * @return                      TRUE if the monitor was started
     */
    public static boolean startMonitor(HoldingType holdingType, long holdingId, String property,
                                    long amount, long threshold, int interval, byte[] keySeed) {
        propertiesLoader = CDI.current().select(PropertiesHolder.class).get();
        blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        blockchain = CDI.current().select(BlockchainImpl.class).get();
        transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
        /** Maximum number of monitors */
        MAX_MONITORS = propertiesLoader.getIntProperty("apl.maxNumberOfMonitors");

        //
        // Initialize monitor processing if it hasn't been done yet.  We do this now
        // instead of during ARS initialization so we don't start the monitor thread if it
        // won't be used.
        //
        init();
        long accountId = Account.getId(Crypto.getPublicKey(keySeed));
        //
        // Create the monitor
        //
        FundingMonitor monitor = new FundingMonitor(holdingType, holdingId, property,
                amount, threshold, interval, accountId, keySeed);
        globalSync.readLock();
        try {
            //
            // Locate monitored accounts based on the account property and the setter identifier
            //
            List<MonitoredAccount> accountList = new ArrayList<>();
            try (DbIterator<AccountProperty> it = AccountPropertyTable.getProperties(0, accountId, property, 0, Integer.MAX_VALUE)) {
                while (it.hasNext()) {
                    AccountProperty accountProperty = it.next();
                    MonitoredAccount account = createMonitoredAccount(accountProperty.getRecipientId(),
                            monitor, accountProperty.getValue());
                    accountList.add(account);
                }
            }
            //
            // Activate the monitor and check each monitored account to see if we need to submit
            // an initial fund transaction
            //
            synchronized (monitors) {
                if (monitors.size() > MAX_MONITORS) {
                    throw new RuntimeException("Maximum of " + MAX_MONITORS + " monitors already started");
                }
                if (monitors.contains(monitor)) {
                    LOG.debug(String.format("%s monitor already started for account %s, property '%s', holding %s",
                            holdingType.name(), monitor.accountName, property, Long.toUnsignedString(holdingId)));
                    return false;
                }
                accountList.forEach(account -> {
                    List<MonitoredAccount> activeList = accounts.get(account.accountId);
                    if (activeList == null) {
                        activeList = new ArrayList<>();
                        accounts.put(account.accountId, activeList);
                    }
                    activeList.add(account);
                    pendingEvents.add(account);
                    LOG.debug(String.format("Created %s monitor for target account %s, property '%s', holding %s, "
                                    + "amount %d, threshold %d, interval %d",
                            holdingType.name(), account.accountName, monitor.property, Long.toUnsignedString(monitor.holdingId),
                            account.amount, account.threshold, account.interval));
                });
                monitors.add(monitor);
                LOG.info(String.format("%s monitor started for funding account %s, property '%s', holding %s",
                        holdingType.name(), monitor.accountName, monitor.property, Long.toUnsignedString(monitor.holdingId)));
            }
        } finally {
            globalSync.readUnlock();
        }
        return true;
    }

    /**
     * Create a monitored account
     *
     * The amount, threshold and interval values specified when the monitor was started can be overridden
     * by specifying one or more values in the property value string
     *
     * @param   accountId           Account identifier
     * @param   monitor             Account monitor
     * @param   propertyValue       Account property value
     * @return                      Monitored account
     */
    private static MonitoredAccount createMonitoredAccount(long accountId, FundingMonitor monitor, String propertyValue) {
        long monitorAmount = monitor.amount;
        long monitorThreshold = monitor.threshold;
        int monitorInterval = monitor.interval;
        if (propertyValue != null && !propertyValue.isEmpty()) {
            try {
                Object parsedValue = JSONValue.parseWithException(propertyValue);
                if (!(parsedValue instanceof JSONObject)) {
                    throw new IllegalArgumentException("Property value is not a JSON object");
                }
                JSONObject jsonValue = (JSONObject)parsedValue;
                monitorAmount = getValue(jsonValue.get("amount"), monitorAmount);
                monitorThreshold = getValue(jsonValue.get("threshold"), monitorThreshold);
                monitorInterval = (int)getValue(jsonValue.get("interval"), monitorInterval);
            } catch (IllegalArgumentException | ParseException exc) {
                String errorMessage = String.format("Account %s, property '%s', value '%s' is not valid",
                            Convert2.rsAccount(accountId), monitor.property, propertyValue);
                throw new IllegalArgumentException(errorMessage, exc);
            }
        }
        return new MonitoredAccount(accountId, monitor, monitorAmount, monitorThreshold, monitorInterval);
    }

    /**
     * Convert a JSON parameter to a numeric value
     *
     * @param   jsonValue           The parsed JSON value
     * @param   defaultValue        The default value
     * @return                      The JSON value or the default value
     */
    private static long getValue(Object jsonValue, long defaultValue) {
        if (jsonValue == null) {
            return defaultValue;
        }
        return Convert.parseLong(jsonValue);
    }

    /**
     * Stop all monitors
     *
     * Pending fund transactions will still be processed
     *
     * @return                      Number of monitors stopped
     */
    public static int stopAllMonitors() {
        int stopCount;
        synchronized(monitors) {
            stopCount = monitors.size();
            monitors.clear();
            accounts.clear();
        }
        LOG.info("All monitors stopped");
        return stopCount;
    }

    /**
     * Stop monitor
     *
     * Pending fund transactions will still be processed
     *
     * @param   holdingType         Monitor holding type
     * @param   holdingId           Asset or currency identifier, ignored for APL monitor
     * @param   property            Account property
     * @param   accountId           Fund account identifier
     * @return                      TRUE if the monitor was stopped
     */
    public static boolean stopMonitor(HoldingType holdingType, long holdingId, String property, long accountId) {
        FundingMonitor monitor = null;
        boolean wasStopped = false;
        synchronized(monitors) {
            //
            // Deactivate the monitor
            //
            Iterator<FundingMonitor> monitorIt = monitors.iterator();
            while (monitorIt.hasNext()) {
                monitor = monitorIt.next();
                if (monitor.holdingType == holdingType && monitor.property.equals(property) &&
                        (holdingType == HoldingType.APL || monitor.holdingId == holdingId) &&
                        monitor.accountId == accountId) {
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
                        if (account.monitor == monitor) {
                            accountIt.remove();
                            if (accountList.isEmpty()) {
                                accountListIt.remove();
                            }
                            break;
                        }
                    }
                }
                LOG.info(String.format("%s monitor stopped for fund account %s, property '%s', holding %d",
                    holdingType.name(), monitor.accountName, monitor.property, monitor.holdingId));
            }
        }
        return wasStopped;
    }

    /**
     * Get monitors satisfying the supplied filter
     *
     * @param   filter              Monitor filter
     * @return                      Monitor list
     */
    public static List<FundingMonitor> getMonitors(Filter<FundingMonitor> filter) {
        List<FundingMonitor> result = new ArrayList<>();
        synchronized(monitors) {
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
     * @return                      Account monitor list
     */
    public static List<FundingMonitor> getAllMonitors() {
        List<FundingMonitor> allMonitors = new ArrayList<>();
        synchronized(monitors) {
            allMonitors.addAll(monitors);
        }
        return allMonitors;
    }

    /** Get all monitored accounts for a single monitor
     *
     * @param  monitor              Monitor
     * @return                      List of monitored accounts
     */
    public static List<MonitoredAccount> getMonitoredAccounts(FundingMonitor monitor) {
        List<MonitoredAccount> monitoredAccounts = new ArrayList<>();
        synchronized(monitors) {
            accounts.values().forEach(monitorList -> monitorList.forEach(account -> {
                if (account.monitor.equals(monitor)) {
                    monitoredAccounts.add(account);
                }
            }));
        }
        return monitoredAccounts;
    }

    /**
     * Initialize monitor processing
     */
    private static synchronized void init() {
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
            Thread processingThread = new ProcessEvents();
            processingThread.start();
            //
            // Register our event listeners
            //
            Account.addListener(new AccountEventHandler(), Account.Event.BALANCE);
            Account.addAssetListener(new AssetEventHandler(), Account.Event.ASSET_BALANCE);
            Account.addCurrencyListener(new CurrencyEventHandler(), Account.Event.CURRENCY_BALANCE);
            Account.addPropertyListener(new SetPropertyEventHandler(), Account.Event.SET_PROPERTY);
            Account.addPropertyListener(new DeletePropertyEventHandler(), Account.Event.DELETE_PROPERTY);
            //
            // All done
            //
            started = true;
            LOG.debug("Account monitor initialization completed");
        } catch (RuntimeException exc) {
            stopped = true;
            LOG.error("Account monitor initialization failed", exc);
            throw exc;
        }
    }

    /**
     * Stop monitor processing
     */
    public static void shutdown() {
        if (started && !stopped) {
            stopped = true;
            processSemaphore.release();
        }
    }

    /**
     * Return the hash code
     *
     * @return                      Hash code
     */
    @Override
    public int hashCode() {
        return holdingType.hashCode() + (int)holdingId + property.hashCode() + (int)accountId;
    }

    /**
     * Check if two monitors are equal
     *
     * @param   obj                 Comparison object
     * @return                      TRUE if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (obj != null && (obj instanceof FundingMonitor)) {
            FundingMonitor monitor = (FundingMonitor)obj;
            if (holdingType == monitor.holdingType && holdingId == monitor.holdingId &&
                    property.equals(monitor.property) && accountId == monitor.accountId) {
                isEqual = true;
            }
        }
        return isEqual;
    }

    /**
     * Process pending account event
     */
    @Vetoed
    private static class ProcessEvents extends Thread {

        /**
         * Process pending updates
         */
        @Override
        public void run() {
            LOG.debug("Account monitor thread started");
            List<MonitoredAccount> suspendedEvents = new ArrayList<>();
            try {
                while (true) {
                    //
                    // Wait for a block to be pushed and then process pending account events
                    //
                    processSemaphore.acquire();
                    if (stopped) {
                        LOG.debug("Account monitor thread stopped");
                        break;
                    }
                    MonitoredAccount monitoredAccount;
                    while ((monitoredAccount = pendingEvents.poll()) != null) {
                        try {
                            Account targetAccount = Account.getAccount(monitoredAccount.accountId);
                            Account fundingAccount = Account.getAccount(monitoredAccount.monitor.accountId);
                            if (blockchain.getHeight() - monitoredAccount.height < monitoredAccount.interval) {
                                if (!suspendedEvents.contains(monitoredAccount)) {
                                    suspendedEvents.add(monitoredAccount);
                                }
                            } else if (targetAccount == null) {
                                LOG.error(String.format("Monitored account %s no longer exists",
                                        monitoredAccount.accountName));
                            } else if (fundingAccount == null) {
                                LOG.error(String.format("Funding account %s no longer exists",
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
                            LOG.error(String.format("Unable to process %s event for account %s, property '%s', holding %s",
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
                LOG.debug("Account monitor thread interrupted");
            } catch (Throwable exc) {
                LOG.error("Account monitor thread terminated", exc);
            }
        }
    }

    /**
     * Process a APL event
     *
     * @param   monitoredAccount            Monitored account
     * @param   targetAccount               Target account
     * @param   fundingAccount              Funding account
     * @throws  AplException                Unable to create transaction
     */
    private static void processAplEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
                                            throws AplException {
        FundingMonitor monitor = monitoredAccount.monitor;
        if (targetAccount.getBalanceATM() < monitoredAccount.threshold) {
            Transaction.Builder builder = Transaction.newTransactionBuilder(monitor.publicKey,
                    monitoredAccount.amount, 0, (short)1440, Attachment.ORDINARY_PAYMENT, blockchain.getLastBlockTimestamp());
            builder.recipientId(monitoredAccount.accountId);
            Transaction transaction = builder.build(monitor.keySeed);
            if (Math.addExact(monitoredAccount.amount, transaction.getFeeATM()) > fundingAccount.getUnconfirmedBalanceATM()) {
                LOG.warn(String.format("Funding account %s has insufficient funds; funding transaction discarded",
                        monitor.accountName));
            } else {
                transactionProcessor.broadcast(transaction);
                monitoredAccount.height = blockchain.getHeight();
                LOG.debug(String.format("%s funding transaction %s for %f %s submitted from %s to %s",
                        blockchainConfig.getCoinSymbol(), transaction.getStringId(), (double)monitoredAccount.amount / Constants.ONE_APL,
                        blockchainConfig.getCoinSymbol(), monitor.accountName, monitoredAccount.accountName));
            }
        }
    }

    /**
     * Process an ASSET event
     *
     * @param   monitoredAccount            Monitored account
     * @param   targetAccount               Target account
     * @param   fundingAccount              Funding account
     * @throws  AplException                Unable to create transaction
     */
    private static void processAssetEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
                                            throws AplException {
        FundingMonitor monitor = monitoredAccount.monitor;
        AccountAsset targetAsset = AccountAssetTable.getAccountAsset(targetAccount.getId(), monitor.holdingId);
        AccountAsset fundingAsset = AccountAssetTable.getAccountAsset(fundingAccount.getId(), monitor.holdingId);
        if (fundingAsset == null || fundingAsset.getUnconfirmedQuantityATU() < monitoredAccount.amount) {
            LOG.warn(
                    String.format("Funding account %s has insufficient quantity for asset %s; funding transaction discarded",
                            monitor.accountName, Long.toUnsignedString(monitor.holdingId)));
        } else if (targetAsset == null || targetAsset.getQuantityATU() < monitoredAccount.threshold) {
            Attachment attachment = new ColoredCoinsAssetTransfer(monitor.holdingId, monitoredAccount.amount);
            Transaction.Builder builder = Transaction.newTransactionBuilder(monitor.publicKey,
                    0, 0, (short)1440, attachment, blockchain.getLastBlockTimestamp());
            builder.recipientId(monitoredAccount.accountId);
            Transaction transaction = builder.build(monitor.keySeed);
            if (transaction.getFeeATM() > fundingAccount.getUnconfirmedBalanceATM()) {
                LOG.warn(String.format("Funding account %s has insufficient funds; funding transaction discarded",
                        monitor.accountName));
            } else {
                transactionProcessor.broadcast(transaction);
                monitoredAccount.height = blockchain.getHeight();
                LOG.debug(String.format("ASSET funding transaction %s submitted for %d units from %s to %s",
                        transaction.getStringId(), monitoredAccount.amount,
                        monitor.accountName, monitoredAccount.accountName));
            }
        }
    }

    /**
     * Process a CURRENCY event
     *
     * @param   monitoredAccount            Monitored account
     * @param   targetAccount               Target account
     * @param   fundingAccount              Funding account
     * @throws  AplException                Unable to create transaction
     */
    private static void processCurrencyEvent(MonitoredAccount monitoredAccount, Account targetAccount, Account fundingAccount)
                                            throws AplException {
        FundingMonitor monitor = monitoredAccount.monitor;
        AccountCurrency targetCurrency = AccountCurrencyTable.getAccountCurrency(targetAccount.getId(), monitor.holdingId);
        AccountCurrency fundingCurrency = AccountCurrencyTable.getAccountCurrency(fundingAccount.getId(), monitor.holdingId);
        if (fundingCurrency == null || fundingCurrency.getUnconfirmedUnits() < monitoredAccount.amount) {
            LOG.warn(
                    String.format("Funding account %s has insufficient quantity for currency %s; funding transaction discarded",
                            monitor.accountName, Long.toUnsignedString(monitor.holdingId)));
        } else if (targetCurrency == null || targetCurrency.getUnits() < monitoredAccount.threshold) {
            Attachment attachment = new MonetarySystemCurrencyTransfer(monitor.holdingId, monitoredAccount.amount);
            Transaction.Builder builder = Transaction.newTransactionBuilder(monitor.publicKey,
                    0, 0, (short)1440, attachment, blockchain.getLastBlockTimestamp());
            builder.recipientId(monitoredAccount.accountId);
            Transaction transaction = builder.build(monitor.keySeed);
            if (transaction.getFeeATM() > fundingAccount.getUnconfirmedBalanceATM()) {
                LOG.warn(String.format("Funding account %s has insufficient funds; funding transaction discarded",
                        monitor.accountName));
            } else {
                transactionProcessor.broadcast(transaction);
                monitoredAccount.height = blockchain.getHeight();
                LOG.debug(String.format("CURRENCY funding transaction %s submitted for %d units from %s to %s",
                        transaction.getStringId(), monitoredAccount.amount,
                        monitor.accountName, monitoredAccount.accountName));
            }
        }
    }

    /**
     * Monitored account
     */
    public static final class MonitoredAccount {

        /** Account identifier */
        private final long accountId;

        /** Account name */
        private final String accountName;

        /** Associated monitor */
        private final FundingMonitor monitor;

        /** Fund amount */
        private long amount;

        /** Fund threshold */
        private long threshold;

        /** Fund interval */
        private  int interval;

        /** Last fund height */
        private int height;

        /**
         * Create a new monitored account
         *
         * @param   accountId           Account identifier
         * @param   monitor             Account monitor
         * @param   amount              Fund amount
         * @param   threshold           Fund threshold
         * @param   interval            Fund interval
         */
        private MonitoredAccount(long accountId, FundingMonitor monitor, long amount, long threshold, int interval) {
            if (amount < MIN_FUND_AMOUNT) {
                throw new IllegalArgumentException("Minimum fund amount is " + MIN_FUND_AMOUNT);
            }
            if (threshold < MIN_FUND_THRESHOLD) {
                throw new IllegalArgumentException("Minimum fund threshold is " + MIN_FUND_THRESHOLD);
            }
            if (interval < MIN_FUND_INTERVAL) {
                throw new IllegalArgumentException("Minimum fund interval is " + MIN_FUND_INTERVAL);
            }
            this.accountId = accountId;
            this.accountName = Convert2.rsAccount(accountId);
            this.monitor = monitor;
            this.amount = amount;
            this.threshold = threshold;
            this.interval = interval;
        }

        /**
         * Get the account identifier
         *
         * @return                      Account identifier
         */
        public long getAccountId() {
            return accountId;
        }

        /**
         * Get the account name (Reed-Solomon encoded account identifier)
         *
         * @return                      Account name
         */
        public String getAccountName() {
            return accountName;
        }

        /**
         * Get the funding amount
         *
         * @return                      Funding amount
         */
        public long getAmount() {
            return amount;
        }

        /**
         * Get the funding threshold
         *
         * @return                      Funding threshold
         */
        public long getThreshold() {
            return threshold;
        }

        /**
         * Get the funding interval
         *
         * @return                      Funding interval
         */
        public int getInterval() {
            return interval;
        }
    }

    /**
     * Account event handler (BALANCE event)
     */
    private static final class AccountEventHandler implements Listener<Account> {

        /**
         * Account event notification
         *
         * @param   account                 Account
         */
        @Override
        public void notify(Account account) {
            if (stopped) {
                return;
            }
            long balance = account.getBalanceATM();
            //
            // Check the APL balance for monitored accounts
            //
            synchronized(monitors) {
                List<MonitoredAccount> accountList = accounts.get(account.getId());
                if (accountList != null) {
                    accountList.forEach((maccount) -> {
                       if (maccount.monitor.holdingType == HoldingType.APL && balance < maccount.threshold &&
                               !pendingEvents.contains(maccount)) {
                           pendingEvents.add(maccount);
                       }
                    });
                }
            }
        }
    }

    /**
     * Asset event handler (ASSET_BALANCE event)
     */
    private static final class AssetEventHandler implements Listener<AccountAsset> {

        /**
         * Asset event notification
         *
         * @param   asset                   Account asset
         */
        @Override
        public void notify(AccountAsset asset) {
            if (stopped) {
                return;
            }
            long balance = asset.getQuantityATU();
            long assetId = asset.getAssetId();
            //
            // Check the asset balance for monitored accounts
            //
            synchronized(monitors) {
                List<MonitoredAccount> accountList = accounts.get(asset.getAccountId());
                if (accountList != null) {
                    accountList.forEach((maccount) -> {
                        if (maccount.monitor.holdingType == HoldingType.ASSET &&
                                maccount.monitor.holdingId == assetId &&
                                balance < maccount.threshold &&
                                !pendingEvents.contains(maccount)) {
                            pendingEvents.add(maccount);
                        }
                    });
                }
            }
        }
    }

    /**
     * Currency event handler (CURRENCY_BALANCE event)
     */
    private static final class CurrencyEventHandler implements Listener<AccountCurrency> {

        /**
         * Currency event notification
         *
         * @param   currency                Account currency
         */
        @Override
        public void notify(AccountCurrency currency) {
            if (stopped) {
                return;
            }
            long balance = currency.getUnits();
            long currencyId = currency.getCurrencyId();
            //
            // Check the currency balance for monitored accounts
            //
            synchronized(monitors) {
                List<MonitoredAccount> accountList = accounts.get(currency.getAccountId());
                if (accountList != null) {
                    accountList.forEach((maccount) -> {
                        if (maccount.monitor.holdingType == HoldingType.CURRENCY &&
                                maccount.monitor.holdingId == currencyId &&
                                balance < maccount.threshold &&
                                !pendingEvents.contains(maccount)) {
                            pendingEvents.add(maccount);
                        }
                    });
                }
            }
        }
    }

    /**
     * Property event handler (SET_PROPERTY event)
     */
    private static final class SetPropertyEventHandler implements Listener<AccountProperty> {

        /**
         * Property event notification
         *
         * @param   property                Account property
         */
        @Override
        public void notify(AccountProperty property) {
            if (stopped) {
                return;
            }
            long accountId = property.getRecipientId();
            try {
                boolean addMonitoredAccount = true;
                synchronized(monitors) {
                    //
                    // Check if updating an existing monitored account.  In this case, we don't need to create
                    // a new monitored account and just need to update any monitor overrides.
                    //
                    List<MonitoredAccount> accountList = accounts.get(accountId);
                    if (accountList != null) {
                        for (MonitoredAccount account : accountList) {
                            if (account.monitor.property.equals(property.getProperty())) {
                                addMonitoredAccount = false;
                                MonitoredAccount newAccount = createMonitoredAccount(accountId, account.monitor, property.getValue());
                                account.amount = newAccount.amount;
                                account.threshold = newAccount.threshold;
                                account.interval = newAccount.interval;
                                pendingEvents.add(account);
                                LOG.debug(
                                        String.format("Updated %s monitor for account %s, property '%s', holding %s, "
                                                + "amount %d, threshold %d, interval %d",
                                                account.monitor.holdingType.name(), account.accountName,
                                                property.getProperty(), Long.toUnsignedString(account.monitor.holdingId),
                                                account.amount, account.threshold, account.interval));
                            }
                        }
                    }
                    //
                    // Create a new monitored account if there is an active monitor for this account property
                    //
                    if (addMonitoredAccount) {
                        for (FundingMonitor monitor : monitors) {
                            if (monitor.property.equals(property.getProperty())) {
                                MonitoredAccount account = createMonitoredAccount(accountId, monitor, property.getValue());
                                accountList = accounts.get(accountId);
                                if (accountList == null) {
                                    accountList = new ArrayList<>();
                                    accounts.put(accountId, accountList);
                                }
                                accountList.add(account);
                                pendingEvents.add(account);
                                LOG.debug(
                                        String.format("Created %s monitor for account %s, property '%s', holding %s, "
                                                + "amount %d, threshold %d, interval %d",
                                                monitor.holdingType.name(), account.accountName,
                                                property.getProperty(), Long.toUnsignedString(monitor.holdingId),
                                                account.amount, account.threshold, account.interval));
                            }
                        }
                    }
                }
            } catch (Exception exc) {
                LOG.error("Unable to process SET_PROPERTY event for account " + Convert2.rsAccount(accountId), exc);
            }
        }
    }

    /**
     * Property event handler (DELETE_PROPERTY event)
     */
    private static final class DeletePropertyEventHandler implements Listener<AccountProperty> {

        /**
         * Property event notification
         *
         * @param   property                Account property
         */
        @Override
        public void notify(AccountProperty property) {
            if (stopped) {
                return;
            }
            long accountId = property.getRecipientId();
            synchronized(monitors) {
                List<MonitoredAccount> accountList = accounts.get(accountId);
                if (accountList != null) {
                    Iterator<MonitoredAccount> it = accountList.iterator();
                    while (it.hasNext()) {
                        MonitoredAccount account = it.next();
                        if (account.monitor.property.equals(property.getProperty())) {
                            it.remove();
                            LOG.debug(
                                    String.format("Deleted %s monitor for account %s, property '%s', holding %s",
                                            account.monitor.holdingType.name(), account.accountName,
                                            property.getProperty(), Long.toUnsignedString(account.monitor.holdingId)));
                        }
                    }
                    if (accountList.isEmpty()) {
                        accounts.remove(accountId);
                    }
                }
            }
        }
    }

    /**
     * Block event handler (BLOCK_PUSHED event)
     *
     * We will process pending funding events when a block is pushed to the blockchain.  This ensures that all
     * block transactions have been processed before we process funding events.
     */
    @Singleton
    public static class BlockEventHandler {

        /**
         * Block event notification
         */
        public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
            if (!stopped && !pendingEvents.isEmpty()) {
                processSemaphore.release();
            }
        }
    }
}
