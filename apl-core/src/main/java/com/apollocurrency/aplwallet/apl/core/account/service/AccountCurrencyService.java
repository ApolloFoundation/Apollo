/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEvent;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.Listener;
import com.apollocurrency.aplwallet.apl.util.Listeners;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountCurrencyService {

    Listeners<AccountCurrency, AccountEvent> currencyListeners = new Listeners<>();

    static boolean addCurrencyListener(Listener<AccountCurrency> listener, AccountEvent eventType) {
        return currencyListeners.addListener(listener, eventType);
    }

    static boolean removeCurrencyListener(Listener<AccountCurrency> listener, AccountEvent eventType) {
        return currencyListeners.removeListener(listener, eventType);
    }

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
