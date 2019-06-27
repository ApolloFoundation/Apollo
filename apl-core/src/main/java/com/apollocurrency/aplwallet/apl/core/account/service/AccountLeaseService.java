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


    List<AccountLease> getLeaseChangingAccounts(int height);

    void leaseEffectiveBalance(Account account, long lesseeId, int period);
}
