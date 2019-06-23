package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountLeaseService {

    Listeners<AccountLease, Account.Event> leaseListeners = new Listeners<>();
    static boolean addLeaseListener(Listener<AccountLease> listener, Account.Event eventType) {
        return leaseListeners.addListener(listener, eventType);
    }

    static boolean removeLeaseListener(Listener<AccountLease> listener, Account.Event eventType) {
        return leaseListeners.removeListener(listener, eventType);
    }

    AccountLease getAccountLease(AccountEntity account);


    List<AccountLease> getLeaseChangingAccounts(int height);

    void leaseEffectiveBalance(AccountEntity account, long lesseeId, int period);
}
