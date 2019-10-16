package com.apollocurrency.aplwallet.apl.core.account.observer;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.shard.DbHotSwapConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRecipientsAttachment;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * @author al
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountObserver {

    private AccountService accountService;

    private AccountLeaseService accountLeaseService;

    private AccountPublicKeyService accountPublicKeyService;

    private Event<AccountLease> accountLeaseEvent;

    private AccountLedgerService accountLedgerService;

    @Inject
    public AccountObserver(AccountService accountService,
                           AccountLeaseService accountLeaseService,
                           AccountPublicKeyService accountPublicKeyService,
                           Event<AccountLease> accountLeaseEvent,
                           AccountLedgerService accountLedgerService) {
        this.accountService = accountService;
        this.accountLeaseService = accountLeaseService;
        this.accountPublicKeyService = accountPublicKeyService;
        this.accountLeaseEvent = accountLeaseEvent;
        this.accountLedgerService = accountLedgerService;
    }

    public void onRescanBegan(@Observes @BlockEvent(BlockEventType.RESCAN_BEGIN) Block block) {
        clearCache();
    }

    public void onDbHotSwapBegin(@Observes DbHotSwapConfig dbHotSwapConfig) {
        clearCache();
    }

    private void clearCache() {
        if (accountPublicKeyService.isCacheEnabled()) {
            accountPublicKeyService.clearCache();
        }
    }

    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block block) {
        log.trace("Catch event (BLOCK_POPPED) {}", block);
        if (accountPublicKeyService.isCacheEnabled()) {
            //TODO: make cache injectable
            accountPublicKeyService.removeFromCache(AccountTable.newKey(block.getGeneratorId()));
            block.getOrLoadTransactions().forEach(transaction -> {
                accountPublicKeyService.removeFromCache(AccountTable.newKey(transaction.getSenderId()));
                if (!transaction.getAppendages(appendix -> (appendix instanceof PublicKeyAnnouncementAppendix), false).isEmpty()) {
                    accountPublicKeyService.removeFromCache(AccountTable.newKey(transaction.getRecipientId()));
                }
                if (transaction.getType() == ShufflingTransaction.SHUFFLING_RECIPIENTS) {
                    ShufflingRecipientsAttachment shufflingRecipients = (ShufflingRecipientsAttachment) transaction.getAttachment();
                    for (byte[] publicKey : shufflingRecipients.getRecipientPublicKeys()) {
                        accountPublicKeyService.removeFromCache(AccountTable.newKey(AccountService.getId(publicKey)));
                    }
                }
            });
        }
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        log.trace("Catch event (AFTER_BLOCK_APPLY) {}", block);
        int height = block.getHeight();
        List<AccountLease> changingLeases = accountLeaseService.getLeaseChangingAccounts(height);
        for (AccountLease lease : changingLeases) {
            Account lessor = accountService.getAccount(lease.getLessorId());
            if (height == lease.getCurrentLeasingHeightFrom()) {
                lessor.setActiveLesseeId(lease.getCurrentLesseeId());
                //leaseListeners.notify(lease, AccountEventType.LEASE_STARTED);
                accountLeaseEvent.select(AccountEventBinding.literal(AccountEventType.LEASE_STARTED)).fire(lease);
            } else if (height == lease.getCurrentLeasingHeightTo()) {
                //leaseListeners.notify(lease, AccountEventType.LEASE_ENDED);
                accountLeaseEvent.select(AccountEventBinding.literal(AccountEventType.LEASE_ENDED)).fire(lease);
                lessor.setActiveLesseeId(0);
                if (lease.getNextLeasingHeightFrom() == 0) {
                    lease.setCurrentLeasingHeightFrom(0);
                    lease.setCurrentLeasingHeightTo(0);
                    lease.setCurrentLesseeId(0);
                    accountLeaseService.deleteLease(lease);
                } else {
                    lease.setCurrentLeasingHeightFrom(lease.getNextLeasingHeightFrom());
                    lease.setCurrentLeasingHeightTo(lease.getNextLeasingHeightTo());
                    lease.setCurrentLesseeId(lease.getNextLesseeId());
                    lease.setNextLeasingHeightFrom(0);
                    lease.setNextLeasingHeightTo(0);
                    lease.setNextLesseeId(0);
                    accountLeaseService.insertLease(lease);
                    if (height == lease.getCurrentLeasingHeightFrom()) {
                        lessor.setActiveLesseeId(lease.getCurrentLesseeId());
                        //leaseListeners.notify(lease, AccountEventType.LEASE_STARTED);
                        accountLeaseEvent.select(AccountEventBinding.literal(AccountEventType.LEASE_STARTED)).fire(lease);
                    }
                }
            }
            accountService.update(lessor);
        }
    }

    public void onLedgerCommitEntries(@Observes @AccountLedgerEvent(AccountLedgerEventType.COMMIT_ENTRIES) AccountLedgerEventType event ){
        log.trace("Catch event (COMMIT_ENTRIES) {}", event);
        accountLedgerService.commitEntries();
    }

    public void onLedgerClearEntries(@Observes @AccountLedgerEvent(AccountLedgerEventType.CLEAR_ENTRIES) AccountLedgerEventType event){
        log.trace("Catch event (CLEAR_ENTRIES) {}", event);
        accountLedgerService.clearEntries();
    }

    public void onLogLedgerEntries(@Observes @AccountLedgerEvent(AccountLedgerEventType.LOG_ENTRY) LedgerEntry entry ){
        log.trace("Catch event (LOG_ENTRY) {}", entry);
        if (accountLedgerService.mustLogEntry(entry.getAccountId(), false)) {
            accountLedgerService.logEntry(entry);
        }else{
            log.trace("The account {} mustn't be tracked", entry.getAccountId());
        }
    }

    public void onLogUnconfirmedLedgerEntries(@Observes @AccountLedgerEvent(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY) LedgerEntry entry ){
        log.trace("Catch event (LOG_UNCONFIRMED_ENTRY) {}", entry);
        if (accountLedgerService.mustLogEntry(entry.getAccountId(), true)) {
            accountLedgerService.logEntry(entry);
        }else{
            log.trace("The account {} mustn't be tracked", entry.getAccountId());
        }
    }
}
