/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountCurrencyService {

    void save(AccountCurrency currency);

    AccountCurrency getCurrency(Account account, long currencyId);

    AccountCurrency getCurrency(Account account, long currencyId, int height);

    DbIterator<AccountCurrency> getCurrencies(Account account, int from, int to);

    DbIterator<AccountCurrency> getCurrencies(Account account, int height, int from, int to);

    long getCurrencyUnits(Account account, long currencyId);

    long getCurrencyUnits(Account account, long currencyId, int height);

    long getUnconfirmedCurrencyUnits(Account account, long currencyId);

    void addToCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units);

    void addToUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units);

    void addToCurrencyAndUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units);
}
