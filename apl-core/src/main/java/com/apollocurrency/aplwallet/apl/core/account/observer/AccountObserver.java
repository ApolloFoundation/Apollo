package com.apollocurrency.aplwallet.apl.core.account.observer;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * @author al
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountObserver {

    private final AccountService accountService;
    private final AccountLeaseService accountLeaseService;
    private final Event<AccountLease> accountLeaseEvent;
    private final AccountLedgerService accountLedgerService;

    @Inject
    public AccountObserver(AccountService accountService,
                           AccountLeaseService accountLeaseService,
                           Event<AccountLease> accountLeaseEvent,
                           AccountLedgerService accountLedgerService) {
        this.accountService = accountService;
        this.accountLeaseService = accountLeaseService;
        this.accountLeaseEvent = accountLeaseEvent;
        this.accountLedgerService = accountLedgerService;
    }

    public void onBlockApplied(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_APPLY) Block block) {
        log.trace(":accept:AccountObserver: START onBlockApplaid AFTER_BLOCK_APPLY. block.height={}", block.getHeight());
        int height = block.getHeight();
        List<AccountLease> changingLeases = accountLeaseService.getLeaseChangingAccountsAtHeight(height);
        for (AccountLease lease : changingLeases) {
            Account lessor = accountService.getAccount(lease.getLessorId());
            if (height == lease.getCurrentLeasingHeightFrom()) {
                lessor.setActiveLesseeId(lease.getCurrentLesseeId());
                if (log.isTraceEnabled()){
                    log.trace("--lease-- set activeLeaseId={}", lease.getCurrentLesseeId());
                }
                accountLeaseEvent.select(AccountEventBinding.literal(AccountEventType.LEASE_STARTED)).fire(lease);
            } else if (height == lease.getCurrentLeasingHeightTo()) {
                accountLeaseEvent.select(AccountEventBinding.literal(AccountEventType.LEASE_ENDED)).fire(lease);
                lessor.setActiveLesseeId(0);
                if (log.isTraceEnabled()){
                    log.trace("--lease-- set activeLeaseId=0");
                }
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
                        if (log.isTraceEnabled()){
                            log.trace("--lease-- set activeLeaseId={}", lease.getCurrentLesseeId());
                        }
                        accountLeaseEvent.select(AccountEventBinding.literal(AccountEventType.LEASE_STARTED)).fire(lease);
                    }
                }
            }
            if (log.isTraceEnabled()){
                log.trace("--lease-- update account, entity={}", lessor);
            }
            accountService.update(lessor);
        }
        log.trace(":accept:AccountObserver: END onBlockApplaid AFTER_BLOCK_APPLY. block={}", block.getHeight());
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
