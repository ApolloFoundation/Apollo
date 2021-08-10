/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.internal;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.smc.data.type.Address;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface CachedAccountService {

    Account getAccount(Address address);

    void addToBalanceAndUnconfirmedBalanceATM(Address address, BigInteger amount);

}
