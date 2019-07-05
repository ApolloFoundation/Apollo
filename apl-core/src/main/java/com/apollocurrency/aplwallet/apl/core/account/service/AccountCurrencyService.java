/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountCurrencyService {

    void save(AccountCurrency currency);

    AccountCurrency getAccountCurrency(Account account, long currencyId);

    AccountCurrency getAccountCurrency(long accountId, long currencyId);

    AccountCurrency getAccountCurrency(long accountId, long currencyId, int height);

    AccountCurrency getAccountCurrency(Account account, long currencyId, int height);

    int getCurrencyAccountCount(long currencyId);

    int getCurrencyAccountCount(long currencyId, int height);

    int getAccountCurrencyCount(long accountId);

    int getAccountCurrencyCount(long accountId, int height);

    List<AccountCurrency> getCurrencies(Account account, int from, int to);

    List<AccountCurrency> getCurrencies(long accountId, int from, int to);

    List<AccountCurrency> getCurrencies(Account account, int height, int from, int to);

    List<AccountCurrency> getCurrencies(long accountId, int height, int from, int to);

    List<AccountCurrency> getCurrencyAccounts(long currencyId, int from, int to);

    List<AccountCurrency> getCurrencyAccounts(long currencyId, int height, int from, int to);

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
