/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountLeaseService {

    AccountLease getAccountLease(AccountEntity account);


    List<AccountLease> getLeaseChangingAccounts(int height);

    void leaseEffectiveBalance(AccountEntity account, long lesseeId, int period);
}
