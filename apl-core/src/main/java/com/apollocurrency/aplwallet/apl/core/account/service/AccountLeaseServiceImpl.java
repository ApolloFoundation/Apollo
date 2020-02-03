/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountLeaseServiceImpl implements AccountLeaseService {

    private final AccountLeaseTable accountLeaseTable;
    private final Blockchain blockchain;
    private final BlockchainConfig blockchainConfig;
    private final Event<AccountLease> accountLeaseEvent;

    @Inject
    public AccountLeaseServiceImpl(AccountLeaseTable accountLeaseTable, Blockchain blockchain, BlockchainConfig blockchainConfig, Event<AccountLease> accountLeaseEvent) {
        this.accountLeaseTable = accountLeaseTable;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.accountLeaseEvent = accountLeaseEvent;
    }

    public void onLeaseStarted(@Observes @AccountEvent(AccountEventType.LEASE_STARTED) AccountLease accountLease ){
        if(log.isTraceEnabled()) {
            log.trace("--lease-- Catch event (LEASE_STARTED) lessor={} at height {}", accountLease, blockchain.getHeight());
        }
    }

    public void onLeaseEnded(@Observes @AccountEvent(AccountEventType.LEASE_ENDED) AccountLease accountLease ){
        if(log.isTraceEnabled()) {
            log.trace("--lease-- Catch event (LEASE_ENDED) lessor={} at height {}", accountLease, blockchain.getHeight());
        }
    }

    public void onLeaseRollbackEnded(@Observes @AccountEvent(AccountEventType.LEASE_ROLLBACK_ENDED) AccountEventType event ){
        if(log.isTraceEnabled()) {
            log.trace("--lease-- Catch event (ROLLBACK_LEASE_ENDED) at height {}", blockchain.getHeight());
        }
    }

    @Override
    public AccountLease getAccountLease(Account account) {
        return accountLeaseTable.get(AccountTable.newKey(account));
    }

    @Override
    public int getAccountLeaseCount() {
        return accountLeaseTable.getAccountLeaseCount();
    }

    @Override
    public void insertLease(AccountLease lease) {
        lease.setHeight(blockchain.getHeight());
        accountLeaseTable.insert(lease);
        if (log.isTraceEnabled()){
            log.trace("--lease-- Insert lease AccountLease={} at height {}", lease, blockchain.getHeight());
        }
    }

    @Override
    public boolean deleteLease(AccountLease lease) {
        if (log.isTraceEnabled()){
            log.trace("--lease-- Delete lease AccountLease={} at height {}", lease, blockchain.getHeight());
        }
        return accountLeaseTable.delete(lease);
    }

    @Override
    public List<AccountLease> getLeaseChangingAccountsAtHeight(int height) {
        return accountLeaseTable.getLeaseChangingAccountsAtHeight(height);
    }

    @Override
    public void leaseEffectiveBalance(Account account, long lesseeId, int period) {
        int height = blockchain.getHeight();
        AccountLease accountLease = accountLeaseTable.get(AccountTable.newKey(account));
        int leasingDelay = blockchainConfig.getLeasingDelay();
        if (accountLease == null) {
            accountLease = new AccountLease(account.getId(),
                    height + leasingDelay,
                    height + leasingDelay + period,
                lesseeId, blockchain.getHeight());
        } else if (accountLease.getCurrentLesseeId() == 0) {
            accountLease.setCurrentLeasingHeightFrom(height + leasingDelay);
            accountLease.setCurrentLeasingHeightTo(height + leasingDelay + period);
            accountLease.setCurrentLesseeId(lesseeId);
        } else {
            accountLease.setNextLeasingHeightFrom(height + leasingDelay);
            if (accountLease.getNextLeasingHeightFrom() < accountLease.getCurrentLeasingHeightTo()) {
                accountLease.setNextLeasingHeightFrom(accountLease.getCurrentLeasingHeightTo());
            }
            accountLease.setNextLeasingHeightTo(accountLease.getNextLeasingHeightFrom() + period);
            accountLease.setNextLesseeId(lesseeId);
        }
        insertLease(accountLease);
        if (log.isTraceEnabled()){
            log.trace("--lease-- Scheduled lease AccountLease={} at height {}", accountLease, blockchain.getHeight());
        }
        accountLeaseEvent.select(literal(AccountEventType.LEASE_SCHEDULED)).fire(accountLease);
    }

}
