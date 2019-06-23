package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    @Override
    public AccountLease getAccountLease(AccountEntity account) {
        return accountLeaseTable.get(AccountTable.newKey(account));
    }

    @Override
    public List<AccountLease> getLeaseChangingAccounts(int height) {
        List<AccountLease> result = new ArrayList<>();
        DbIterator<AccountLease> leases = accountLeaseTable.getLeaseChangingAccounts(height);
        leases.forEachRemaining(result::add);

        return result;
    }

    @Override
    public void leaseEffectiveBalance(AccountEntity account, long lesseeId, int period) {
        int height = blockchain.getHeight();
        AccountLease accountLease = AccountLeaseTable.getInstance().get(AccountTable.newKey(account));
        int leasingDelay = blockchainConfig.getLeasingDelay();
        if (accountLease == null) {
            accountLease = new AccountLease(account.getId(),
                    height + leasingDelay,
                    height + leasingDelay + period,
                    lesseeId);
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
        leaseListeners.notify(accountLease, Account.Event.LEASE_SCHEDULED);
    }


}
