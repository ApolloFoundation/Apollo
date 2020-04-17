/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountLeaseService {

    AccountLease getAccountLease(Account account);


    int getAccountLeaseCount();

    void insertLease(AccountLease lease);

    boolean deleteLease(AccountLease lease);

    List<AccountLease> getLeaseChangingAccountsAtHeight(int height);

    /**
     * Lease own's forging power to another account for a fixed period of time.
     * When the leasing period expires, the forging power will return back automatically.
     *
     * @param account  it's forging power is allowed to lease
     * @param lesseeId another account
     * @param period   a certain fixed period time
     */
    void leaseEffectiveBalance(Account account, long lesseeId, int period);
}
