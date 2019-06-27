/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountCurrencyService {

    void save(AccountCurrency currency);

    AccountCurrency getCurrency(AccountEntity account, long currencyId);

    AccountCurrency getCurrency(AccountEntity account, long currencyId, int height);

    DbIterator<AccountCurrency> getCurrencies(AccountEntity account, int from, int to);

    DbIterator<AccountCurrency> getCurrencies(AccountEntity account, int height, int from, int to);

    long getCurrencyUnits(AccountEntity account, long currencyId);

    long getCurrencyUnits(AccountEntity account, long currencyId, int height);

    long getUnconfirmedCurrencyUnits(AccountEntity account, long currencyId);

    void addToCurrencyUnits(AccountEntity account, LedgerEvent event, long eventId, long currencyId, long units);

    void addToUnconfirmedCurrencyUnits(AccountEntity account, LedgerEvent event, long eventId, long currencyId, long units);

    void addToCurrencyAndUnconfirmedCurrencyUnits(AccountEntity account, LedgerEvent event, long eventId, long currencyId, long units);
}
