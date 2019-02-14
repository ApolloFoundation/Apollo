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

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

/**
 * Maintain a ledger of changes to selected accounts
 */
public class AccountLedger {
        private static final Logger LOG = getLogger(AccountLedger.class);


    /** Account ledger is enabled */
    private static boolean ledgerEnabled;

    /** Track all accounts */
    private static boolean trackAllAccounts;

    /** Accounts to track */
    private static final SortedSet<Long> trackAccounts = new TreeSet<>();

    /** Unconfirmed logging */
    private static int logUnconfirmed;

    // TODO: YL remove static instance later

   public static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
   public static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

    /** Number of blocks to keep when trimming */
    public static final int trimKeep = propertiesHolder.getIntProperty("apl.ledgerTrimKeep", 30000);

    /** Blockchain */
    private static final Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

    /** Blockchain processor */
    private static final BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
    private static DatabaseManager databaseManager;

    /** Pending ledger entries */
    private static final List<LedgerEntry> pendingEntries = new ArrayList<>();

    @Inject
    public AccountLedger() {
      
    }

    /**
     * Process apl.ledgerAccounts
     */
/*
    static {
        List<String> ledgerAccounts = propertiesHolder.getStringListProperty("apl.ledgerAccounts");
        ledgerEnabled = !ledgerAccounts.isEmpty();
        trackAllAccounts = ledgerAccounts.contains("*");
        if (ledgerEnabled) {
            if (trackAllAccounts) {
                LOG.info("Account ledger is tracking all accounts");
            } else {
                for (String account : ledgerAccounts) {
                    try {
                        trackAccounts.add(Convert.parseAccountId(account));
                        LOG.info("Account ledger is tracking account " + account);
                    } catch (RuntimeException e) {
                        LOG.error("Account " + account + " is not valid; ignored");
                    }
                }
            }
        } else {
            LOG.info("Account ledger is not enabled");
        }
        int temp = propertiesHolder.getIntProperty("apl.ledgerLogUnconfirmed", 1);
        logUnconfirmed = (temp >= 0 && temp <= 2 ? temp : 1);
    }
*/

    private static final AccountLedgerTable accountLedgerTable = new AccountLedgerTable();

    /**
     * Initialization

 We don't do anything but we need to be called from AplCore.init() in order to
 register our table
     */

    public static void init(DatabaseManager databaseManagerParam) {
        databaseManager = databaseManagerParam;

        List<String> ledgerAccounts = propertiesHolder.getStringListProperty("apl.ledgerAccounts");
        ledgerEnabled = !ledgerAccounts.isEmpty();
        trackAllAccounts = ledgerAccounts.contains("*");
        if (ledgerEnabled) {
            if (trackAllAccounts) {
                LOG.info("Account ledger is tracking all accounts");
            } else {
                for (String account : ledgerAccounts) {
                    try {
                        trackAccounts.add(Convert.parseAccountId(account));
                        LOG.info("Account ledger is tracking account " + account);
                    } catch (RuntimeException e) {
                        LOG.error("Account " + account + " is not valid; ignored");
                    }
                }
            }
        } else {
            LOG.info("Account ledger is not enabled");
        }
        int temp = propertiesHolder.getIntProperty("apl.ledgerLogUnconfirmed", 1);
        logUnconfirmed = (temp >= 0 && temp <= 2 ? temp : 1);
    }

    /**
     * Account ledger listener events
     */
    public enum Event {
        ADD_ENTRY
    }

    /**
     * Account ledger listeners
     */
    private static final Listeners<LedgerEntry, Event> listeners = new Listeners<>();

    /**
     * Add a listener
     *
     * @param   listener                    Listener
     * @param   eventType                   Event to listen for
     * @return                              True if the listener was added
     */
    public static boolean addListener(Listener<LedgerEntry> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    /**
     * Remove a listener
     *
     * @param   listener                    Listener
     * @param   eventType                   Event to listen for
     * @return                              True if the listener was removed
     */
    public static boolean removeListener(Listener<LedgerEntry> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    static boolean mustLogEntry(long accountId, boolean isUnconfirmed) {
        //
        // Must be tracking this account
        //
        if (!ledgerEnabled || (!trackAllAccounts && !trackAccounts.contains(accountId))) {
            return false;
        }
        // confirmed changes only occur while processing block, and unconfirmed changes are
        // only logged while processing block
        if (!blockchainProcessor.isProcessingBlock()) {
            return false;
        }
        //
        // Log unconfirmed changes only when processing a block and logUnconfirmed does not equal 0
        // Log confirmed changes unless logUnconfirmed equals 2
        //
        if (isUnconfirmed && logUnconfirmed == 0) {
            return false;
        }
        if (!isUnconfirmed && logUnconfirmed == 2) {
            return false;
        }
        if (trimKeep > 0 && blockchain.getHeight() <= blockchainConfig.getLastKnownBlock() - trimKeep) {
            return false;
        }
        //
        // Don't log account changes if we are scanning the blockchain and the current height
        // is less than the minimum account_ledger trim height
        //
        if (blockchainProcessor.isScanning() && trimKeep > 0 &&
                blockchain.getHeight() <= blockchainProcessor.getInitialScanHeight() - trimKeep) {
            return false;
        }
        return true;
    }

    /**
     * Log an event in the account_ledger table
     *
     * @param   ledgerEntry                 Ledger entry
     */
    static void logEntry(LedgerEntry ledgerEntry) {
        //
        // Must be in a database transaction
        //
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        //
        // Combine multiple ledger entries
        //
        int index = pendingEntries.indexOf(ledgerEntry);
        if (index >= 0) {
            LedgerEntry existingEntry = pendingEntries.remove(index);
            ledgerEntry.updateChange(existingEntry.getChange());
            long adjustedBalance = existingEntry.getBalance() - existingEntry.getChange();
            for (; index < pendingEntries.size(); index++) {
                existingEntry = pendingEntries.get(index);
                if (existingEntry.getAccountId() == ledgerEntry.getAccountId() &&
                        existingEntry.getHolding() == ledgerEntry.getHolding() &&
                        ((existingEntry.getHoldingId() == null && ledgerEntry.getHoldingId() == null) ||
                        (existingEntry.getHoldingId() != null && existingEntry.getHoldingId().equals(ledgerEntry.getHoldingId())))) {
                    adjustedBalance += existingEntry.getChange();
                    existingEntry.setBalance(adjustedBalance);
                }
            }
        }
        pendingEntries.add(ledgerEntry);
    }

    /**
     * Commit pending ledger entries
     */
    public static void commitEntries() {
        int count = 0;
        for (LedgerEntry ledgerEntry : pendingEntries) {
            accountLedgerTable.insert(ledgerEntry);
            listeners.notify(ledgerEntry, Event.ADD_ENTRY);
        }
        pendingEntries.clear();
    }

    /**
     * Clear pending ledger entries
     */
    public static void clearEntries() {
        pendingEntries.clear();
    }

    /**
     * Return a single entry identified by the ledger entry identifier
     *
     * @param   ledgerId                    Ledger entry identifier
     * @param allowPrivate                  Allow requested ledger entry to belong to private transaction or not
     * @return                              Ledger entry or null if entry not found
     */
    public static LedgerEntry getEntry(long ledgerId, boolean allowPrivate) {
        if (!ledgerEnabled)
            return null;
        LedgerEntry entry;
        String sql = "SELECT * FROM account_ledger WHERE db_id = ? ";
        if (!allowPrivate) {
            sql += " AND event_id NOT IN (select event_id from account_ledger where event_type = ? ) ";
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, ledgerId);
            if (!allowPrivate) {
                stmt.setInt(2, LedgerEvent.PRIVATE_PAYMENT.code);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    entry = new LedgerEntry(rs);
                else
                    entry = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return entry;
    }

    /**
     * Return the ledger entries sorted in descending insert order
     *
     *
     * @param   accountId                   Account identifier or zero if no account identifier
     * @param   event                       Ledger event or null
     * @param   eventId                     Ledger event identifier or zero if no event identifier
     * @param   holding                     Ledger holding or null
     * @param   holdingId                   Ledger holding identifier or zero if no holding identifier
     * @param   firstIndex                  First matching entry index, inclusive
     * @param   lastIndex                   Last matching entry index, inclusive
     * @param   includePrivate              Boolean flag that specifies, should response include private ledger entries or not
     * @return                              List of ledger entries
     */
    public static List<LedgerEntry> getEntries(long accountId, LedgerEvent event, long eventId,
                                               LedgerHolding holding, long holdingId,
                                               int firstIndex, int lastIndex, boolean includePrivate) {
        if (!ledgerEnabled) {
            return Collections.emptyList();
        }
        List<LedgerEntry> entryList = new ArrayList<>();
        //
        // Build the SELECT statement to search the entries
        StringBuilder sb = new StringBuilder(128);
        sb.append("SELECT * FROM account_ledger ");
        if (accountId != 0 || event != null || holding != null || !includePrivate) {
            sb.append("WHERE ");
        }
        if (accountId != 0) {
            sb.append("account_id = ? ");
        }
        if (!includePrivate && event == LedgerEvent.PRIVATE_PAYMENT) {
            throw new RuntimeException("None of private ledger entries should be retrieved!");
        }
        if (!includePrivate) {
            if (accountId != 0) {
                sb.append("AND ");
            }
            sb.append("event_id not in (select event_id from account_ledger where ");
            if (accountId != 0)
            {
                sb.append("account_id = ? AND ");
            }
            sb.append("event_type = ? ) ");
        }
        if (event != null) {
            if (accountId != 0 || !includePrivate) {
                sb.append("AND ");
            }
            sb.append("event_type = ? ");
            if (eventId != 0)
                sb.append("AND event_id = ? ");
        }
        if (holding != null) {
            if (accountId != 0 || event != null || !includePrivate) {
                sb.append("AND ");
            }
            sb.append("holding_type = ? ");
            if (holdingId != 0)
                sb.append("AND holding_id = ? ");
        }
        sb.append("ORDER BY db_id DESC ");
        sb.append(DbUtils.limitsClause(firstIndex, lastIndex));
        //
        // Get the ledger entries
        //
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        blockchain.readLock();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sb.toString())) {
            int i = 0;
            if (accountId != 0) {
                pstmt.setLong(++i, accountId);
            }
            if (!includePrivate) {
                if (accountId != 0) {
                    pstmt.setLong(++i, accountId);
                }
                pstmt.setInt(++i, LedgerEvent.PRIVATE_PAYMENT.code);
            }
            if (event != null) {
                pstmt.setByte(++i, (byte)event.getCode());
                if (eventId != 0) {
                    pstmt.setLong(++i, eventId);
                }
            }
            if (holding != null) {
                pstmt.setByte(++i, (byte)holding.getCode());
                if (holdingId != 0) {
                    pstmt.setLong(++i, holdingId);
                }
            }
            DbUtils.setLimits(++i, pstmt, firstIndex, lastIndex);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entryList.add(new LedgerEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            blockchain.readUnlock();
        }
        return entryList;
    }

    /**
     * Ledger events
     *
     * There must be a ledger event defined for each transaction (type,subtype) pair.  When adding
     * a new event, do not change the existing code assignments since these codes are stored in
     * the event_type field of the account_ledger table.
     */
    public enum LedgerEvent {
        // Block and Transaction
            BLOCK_GENERATED(1, false),
            REJECT_PHASED_TRANSACTION(2, true),
            TRANSACTION_FEE(50, true),
        // TYPE_PAYMENT
            ORDINARY_PAYMENT(3, true),
            PRIVATE_PAYMENT(58, true),
        // TYPE_MESSAGING
            ACCOUNT_INFO(4, true),
            ALIAS_ASSIGNMENT(5, true),
            ALIAS_BUY(6, true),
            ALIAS_DELETE(7, true),
            ALIAS_SELL(8, true),
            ARBITRARY_MESSAGE(9, true),
            HUB_ANNOUNCEMENT(10, true),
            PHASING_VOTE_CASTING(11, true),
            POLL_CREATION(12, true),
            VOTE_CASTING(13, true),
            ACCOUNT_PROPERTY(56, true),
            ACCOUNT_PROPERTY_DELETE(57, true),
        // TYPE_COLORED_COINS
            ASSET_ASK_ORDER_CANCELLATION(14, true),
            ASSET_ASK_ORDER_PLACEMENT(15, true),
            ASSET_BID_ORDER_CANCELLATION(16, true),
            ASSET_BID_ORDER_PLACEMENT(17, true),
            ASSET_DIVIDEND_PAYMENT(18, true),
            ASSET_ISSUANCE(19, true),
            ASSET_TRADE(20, true),
            ASSET_TRANSFER(21, true),
            ASSET_DELETE(49, true),
        // TYPE_DIGITAL_GOODS
            DIGITAL_GOODS_DELISTED(22, true),
            DIGITAL_GOODS_DELISTING(23, true),
            DIGITAL_GOODS_DELIVERY(24, true),
            DIGITAL_GOODS_FEEDBACK(25, true),
            DIGITAL_GOODS_LISTING(26, true),
            DIGITAL_GOODS_PRICE_CHANGE(27, true),
            DIGITAL_GOODS_PURCHASE(28, true),
            DIGITAL_GOODS_PURCHASE_EXPIRED(29, true),
            DIGITAL_GOODS_QUANTITY_CHANGE(30, true),
            DIGITAL_GOODS_REFUND(31, true),
        // TYPE_ACCOUNT_CONTROL
            ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING(32, true),
            ACCOUNT_CONTROL_PHASING_ONLY(55, true),
        // TYPE_CURRENCY
            CURRENCY_DELETION(33, true),
            CURRENCY_DISTRIBUTION(34, true),
            CURRENCY_EXCHANGE(35, true),
            CURRENCY_EXCHANGE_BUY(36, true),
            CURRENCY_EXCHANGE_SELL(37, true),
            CURRENCY_ISSUANCE(38, true),
            CURRENCY_MINTING(39, true),
            CURRENCY_OFFER_EXPIRED(40, true),
            CURRENCY_OFFER_REPLACED(41, true),
            CURRENCY_PUBLISH_EXCHANGE_OFFER(42, true),
            CURRENCY_RESERVE_CLAIM(43, true),
            CURRENCY_RESERVE_INCREASE(44, true),
            CURRENCY_TRANSFER(45, true),
            CURRENCY_UNDO_CROWDFUNDING(46, true),
        // TYPE_DATA
            TAGGED_DATA_UPLOAD(47, true),
            TAGGED_DATA_EXTEND(48, true),
        // TYPE_SHUFFLING
            SHUFFLING_REGISTRATION(51, true),
            SHUFFLING_PROCESSING(52, true),
            SHUFFLING_CANCELLATION(53, true),
            SHUFFLING_DISTRIBUTION(54, true),
        // TYPE_UPDATE
            UPDATE_CRITICAL(59, true),
            UPDATE_IMPORTANT(60, true),
            UPDATE_MINOR(61, true);

        /** Event code mapping */
        private static final Map<Integer, LedgerEvent> eventMap = new HashMap<>();
        static {
            for (LedgerEvent event : values()) {
                if (eventMap.put(event.code, event) != null) {
                    throw new RuntimeException("LedgerEvent code " + event.code + " reused");
                }
            }
        }

        /** Event code */
        private final int code;

        /** Event identifier is a transaction */
        private final boolean isTransaction;

        /**
         * Create the ledger event
         *
         * @param   code                    Event code
         * @param   isTransaction           Event identifier is a transaction
         */
        LedgerEvent(int code, boolean isTransaction) {
            this.code = code;
            this.isTransaction = isTransaction;
        }

        /**
         * Check if the event identifier is a transaction
         *
         * @return                          TRUE if the event identifier is a transaction
         */
        public boolean isTransaction() {
            return isTransaction;
        }

        /**
         * Return the event code
         *
         * @return                          Event code
         */
        public int getCode() {
            return code;
        }

        /**
         * Get the event from the event code
         *
         * @param   code                    Event code
         * @return                          Event
         */
        public static LedgerEvent fromCode(int code) {
            LedgerEvent event = eventMap.get(code);
            if (event == null) {
                throw new IllegalArgumentException("LedgerEvent code " + code + " is unknown");
            }
            return event;
        }
    }

    /**
     * Ledger holdings
     *
     * When adding a new holding, do not change the existing code assignments since
     * they are stored in the holding_type field of the account_ledger table.
     */
    public enum LedgerHolding {
        UNCONFIRMED_APL_BALANCE(1, true),
        APL_BALANCE(2, false),
        UNCONFIRMED_ASSET_BALANCE(3, true),
        ASSET_BALANCE(4, false),
        UNCONFIRMED_CURRENCY_BALANCE(5, true),
        CURRENCY_BALANCE(6, false);

        /** Holding code mapping */
        private static final Map<Integer, LedgerHolding> holdingMap = new HashMap<>();
        static {
            for (LedgerHolding holding : values()) {
                if (holdingMap.put(holding.code, holding) != null) {
                    throw new RuntimeException("LedgerHolding code " + holding.code + " reused");
                }
            }
        }

        /** Holding code */
        private final int code;

        /** Unconfirmed holding */
        private final boolean isUnconfirmed;

        /**
         * Create the holding event
         *
         * @param   code                    Holding code
         * @param   isUnconfirmed           TRUE if the holding is unconfirmed
         */
        LedgerHolding(int code, boolean isUnconfirmed) {
            this.code = code;
            this.isUnconfirmed = isUnconfirmed;
        }

        /**
         * Check if the holding is unconfirmed
         *
         * @return                          TRUE if the holding is unconfirmed
         */
        public boolean isUnconfirmed() {
            return this.isUnconfirmed;
        }

        /**
         * Return the holding code
         *
         * @return                          Holding code
         */
        public int getCode() {
            return code;
        }

        /**
         * Get the holding from the holding code
         *
         * @param   code                    Holding code
         * @return                          Holding
         */
        public static LedgerHolding fromCode(int code) {
            LedgerHolding holding = holdingMap.get(code);
            if (holding == null) {
                throw new IllegalArgumentException("LedgerHolding code " + code + " is unknown");
            }
            return holding;
        }
    }

}
