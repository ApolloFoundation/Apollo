/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountCurrencyService {

    void update(AccountCurrency currency);

    AccountCurrency getAccountCurrency(Account account, long currencyId);

    AccountCurrency getAccountCurrency(long accountId, long currencyId);

    AccountCurrency getAccountCurrency(long accountId, long currencyId, int height);

    AccountCurrency getAccountCurrency(Account account, long currencyId, int height);

    int getCountByCurrency(long currencyId);

    int getCountByCurrency(long currencyId, int height);

    int getCountByAccount(long accountId);

    int getCountByAccount(long accountId, int height);

    List<AccountCurrency> getByAccount(Account account, int from, int to);

    List<AccountCurrency> getByAccount(long accountId, int from, int to);

    List<AccountCurrency> getByAccount(Account account, int height, int from, int to);

    List<AccountCurrency> getByAccount(long accountId, int height, int from, int to);

    List<AccountCurrency> getByCurrency(long currencyId, int from, int to);

    List<AccountCurrency> getByCurrency(long currencyId, int height, int from, int to);

    List<AccountCurrency> getByAccount(Account account);

    long getCurrencyUnits(Account account, long currencyId);

    long getCurrencyUnits(long accountId, long currencyId);

    long getCurrencyUnits(Account account, long currencyId, int height);

    long getCurrencyUnits(long accountId, long currencyId, int height);

    long getUnconfirmedCurrencyUnits(Account account, long currencyId);

    long getUnconfirmedCurrencyUnits(long accountId, long currencyId);

    void addToCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units);

    void addToUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units);

    void addToCurrencyAndUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units);
}
