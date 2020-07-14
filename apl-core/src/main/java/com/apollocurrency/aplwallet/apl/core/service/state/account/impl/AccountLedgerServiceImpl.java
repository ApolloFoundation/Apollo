/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountLedgerTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Maintain a ledger of changes to selected accounts
 */
@Slf4j
@Singleton
public class AccountLedgerServiceImpl implements AccountLedgerService {
    /**
     * Account ledger is enabled
     */
    private final boolean ledgerEnabled;

    /**
     * Track all accounts
     */
    private final boolean trackAllAccounts;

    /**
     * Accounts to track
     */
    private final SortedSet<Long> trackAccounts = new TreeSet<>();

    /**
     * Unconfirmed logging
     */
    private final int logUnconfirmed;

    /**
     * Blockchain
     */
    private final Blockchain blockchain;

    /**
     * Blockchain processor
     */
    private final BlockchainProcessor blockchainProcessor;

    private final BlockchainConfig blockchainConfig;

    private final AccountLedgerTable accountLedgerTable;

    /**
     * Pending ledger entries
     */
    @Getter
    private final List<LedgerEntry> pendingEntries = new ArrayList<>();

    private Event<LedgerEntry> accountLedgerEntryEvent;

    @Inject
    public AccountLedgerServiceImpl(AccountLedgerTable accountLedgerTable, Blockchain blockchain, BlockchainProcessor blockchainProcessor, PropertiesHolder propertiesHolder, BlockchainConfig blockchainConfig, Event<LedgerEntry> accountLedgerEntryEvent) {
        this.accountLedgerTable = accountLedgerTable;
        this.blockchain = blockchain;
        this.blockchainProcessor = blockchainProcessor;
        this.blockchainConfig = blockchainConfig;
        this.accountLedgerEntryEvent = accountLedgerEntryEvent;

        List<String> ledgerAccounts = propertiesHolder.getStringListProperty("apl.ledgerAccounts");
        ledgerEnabled = !ledgerAccounts.isEmpty();
        trackAllAccounts = ledgerAccounts.contains("*");
        if (ledgerEnabled) {
            if (trackAllAccounts) {
                log.info("Account ledger is tracking all accounts");
            } else {
                for (String account : ledgerAccounts) {
                    try {
                        trackAccounts.add(Convert.parseAccountId(account));
                        log.info("Account ledger is tracking account " + account);
                    } catch (RuntimeException e) {
                        log.error("Account " + account + " is not valid; ignored");
                    }
                }
            }
        } else {
            log.info("Account ledger is not enabled");
        }
        int temp = propertiesHolder.getIntProperty("apl.ledgerLogUnconfirmed", 1);
        logUnconfirmed = (temp >= 0 && temp <= 2 ? temp : 1);
    }

    /**
     * @return the Number of blocks to keep when trimming
     */
    @Override
    public int getTrimKeep() {
        return accountLedgerTable.getTrimKeep();
    }

    @Override
    public boolean mustLogEntry(long accountId, boolean isUnconfirmed) {
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
        if (getTrimKeep() > 0 && blockchain.getHeight() <= blockchainConfig.getLastKnownBlock() - getTrimKeep()) {
            return false;
        }
        //
        // Don't log account changes if we are scanning the blockchain and the current height
        // is less than the minimum account_ledger trim height
        //
        if (blockchainProcessor.isScanning() && getTrimKeep() > 0 &&
            blockchain.getHeight() <= blockchainProcessor.getInitialScanHeight() - getTrimKeep()) {
            return false;
        }
        return true;
    }

    /**
     * Log an event in the account_ledger table
     *
     * @param ledgerEntry Ledger entry
     */
    @Override
    public void logEntry(LedgerEntry ledgerEntry) {
        //
        // Must be in a database transaction
        //
        if (!accountLedgerTable.isInTransaction()) {
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
    @Override
    public void commitEntries() {
        for (LedgerEntry ledgerEntry : pendingEntries) {
            accountLedgerTable.insert(ledgerEntry);
            accountLedgerEntryEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.ADD_ENTRY)).fire(ledgerEntry);
        }
        pendingEntries.clear();
    }

    /**
     * Clear pending ledger entries
     */
    @Override
    public void clearEntries() {
        pendingEntries.clear();
    }

    /**
     * Return a single entry identified by the ledger entry identifier
     *
     * @param ledgerId     Ledger entry identifier
     * @param allowPrivate Allow requested ledger entry to belong to private transaction or not
     * @return Ledger entry or null if entry not found
     */
    @Override
    public LedgerEntry getEntry(long ledgerId, boolean allowPrivate) {
        if (!ledgerEnabled)
            return null;
        return accountLedgerTable.getEntry(ledgerId, allowPrivate);
    }

    /**
     * Return the ledger entries sorted in descending insert order
     *
     * @param accountId      Account identifier or zero if no account identifier
     * @param event          Ledger event or null
     * @param eventId        Ledger event identifier or zero if no event identifier
     * @param holding        Ledger holding or null
     * @param holdingId      Ledger holding identifier or zero if no holding identifier
     * @param firstIndex     First matching entry index, inclusive
     * @param lastIndex      Last matching entry index, inclusive
     * @param includePrivate Boolean flag that specifies, should response include private ledger entries or not
     * @return List of ledger entries
     */
    @Override
    public List<LedgerEntry> getEntries(long accountId, LedgerEvent event, long eventId,
                                        LedgerHolding holding, long holdingId,
                                        int firstIndex, int lastIndex, boolean includePrivate) {
        if (!ledgerEnabled) {
            return Collections.emptyList();
        }
        return accountLedgerTable.getEntries(accountId, event, eventId, holding, holdingId, firstIndex, lastIndex, includePrivate);
    }

}
