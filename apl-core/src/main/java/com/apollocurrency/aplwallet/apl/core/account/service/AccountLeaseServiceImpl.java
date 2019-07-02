/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import lombok.Setter;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountLeaseServiceImpl implements AccountLeaseService {

    @Inject @Setter
    private AccountLeaseTable accountLeaseTable;

    @Inject @Setter
    private Blockchain blockchain;

    @Inject @Setter
    private BlockchainConfig blockchainConfig;

    @Inject @Setter
    private Event<AccountLease> accountLeaseEvent;

    @Override
    public AccountLease getAccountLease(Account account) {
        return accountLeaseTable.get(AccountTable.newKey(account));
    }

    @Override
    public List<AccountLease> getLeaseChangingAccounts(int height) {
        List<AccountLease> result = new ArrayList<>();
        try(DbIterator<AccountLease> leases = accountLeaseTable.getLeaseChangingAccounts(height)) {
            leases.forEachRemaining(result::add);
        }
        return result;
    }

    @Override
    public void leaseEffectiveBalance(Account account, long lesseeId, int period) {
        int height = blockchain.getHeight();
        AccountLease accountLease = AccountLeaseTable.getInstance().get(AccountTable.newKey(account));
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
        accountLeaseTable.insert(accountLease);
        //leaseListeners.notify(accountLease, AccountEventType.LEASE_SCHEDULED);
        accountLeaseEvent.select(literal(AccountEventType.LEASE_SCHEDULED)).fire(accountLease);
    }


}
