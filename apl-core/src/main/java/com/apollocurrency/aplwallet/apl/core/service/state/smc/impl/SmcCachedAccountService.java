/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc.impl;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.CachedAccountService;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.smc.data.type.Address;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class SmcCachedAccountService implements CachedAccountService {
    private final AccountService accountService;
    private final Map<Long, Account> inMemoryAccounts;

    public SmcCachedAccountService(@NonNull AccountService accountService) {
        this.accountService = accountService;
        this.inMemoryAccounts = new HashMap<>();
    }

    @Override
    public Account getAccount(Address address) {
        if (log.isTraceEnabled()) {
            log.trace("Cached: getAccount address={}", address);
        }
        var longId = new AplAddress(address).getLongId();
        return inMemoryAccounts.computeIfAbsent(longId, accountService::getAccount);
    }

    @Override
    public void addToBalanceAndUnconfirmedBalanceATM(Address address, BigInteger value) {
        var amount = value.longValueExact();
        var account = getAccount(address);
        final long balanceATM = Math.addExact(account.getBalanceATM(), amount);
        final long unconfirmedBalanceATM = Math.addExact(account.getUnconfirmedBalanceATM(), amount);
        if (log.isTraceEnabled()) {
            log.trace("Cached: addBalance account={} balance={} unconfirmed={} balance={} unconfirmed={}"
                , address.getHex()
                , account.getBalanceATM(), account.getUnconfirmedBalanceATM()
                , balanceATM, unconfirmedBalanceATM);
        }
        account.setBalanceATM(balanceATM);
        account.setUnconfirmedBalanceATM(unconfirmedBalanceATM);
        AccountService.checkBalance(account.getId(), account.getBalanceATM(), account.getUnconfirmedBalanceATM());
        //update cached value
        inMemoryAccounts.put(account.getId(), account);
    }
}
