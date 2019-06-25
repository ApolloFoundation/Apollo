/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountLeaseService {

    Listeners<AccountLease, AccountEvent> leaseListeners = new Listeners<>();
    static boolean addLeaseListener(Listener<AccountLease> listener, AccountEvent eventType) {
        return leaseListeners.addListener(listener, eventType);
    }

    static boolean removeLeaseListener(Listener<AccountLease> listener, AccountEvent eventType) {
        return leaseListeners.removeListener(listener, eventType);
    }

    AccountLease getAccountLease(AccountEntity account);


    List<AccountLease> getLeaseChangingAccounts(int height);

    void leaseEffectiveBalance(AccountEntity account, long lesseeId, int period);
}
