/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.*;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import lombok.Setter;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;
import static com.apollocurrency.aplwallet.apl.core.account.service.AccountService.checkBalance;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountCurrencyServiceImpl implements AccountCurrencyService {

    @Inject @Setter
    private AccountCurrencyTable accountCurrencyTable;

    @Inject @Setter
    private AccountService accountService;

    @Inject @Setter
    private Event<Account> accountEvent;

    @Inject @Setter
    private Event<AccountCurrency> accountCurrencyEvent;

    @Override
    public void save(AccountCurrency currency) {
        checkBalance(currency.accountId, currency.units, currency.unconfirmedUnits);
        if (currency.units > 0 || currency.unconfirmedUnits > 0) {
            accountCurrencyTable.insert(currency);
        } else if (currency.units == 0 && currency.unconfirmedUnits == 0) {
            accountCurrencyTable.delete(currency);
        }
    }

    @Override
    public AccountCurrency getCurrency(Account account, long currencyId) {
        return AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(account.getId(), currencyId));
    }

    @Override
    public AccountCurrency getCurrency(Account account, long currencyId, int height) {
        return AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(account.getId(), currencyId), height);
    }

    @Override
    public DbIterator<AccountCurrency> getCurrencies(Account account, int from, int to) {
        return AccountCurrencyTable.getInstance().getManyBy(new DbClause.LongClause("account_id", account.getId()), from, to);
    }

    @Override
    public DbIterator<AccountCurrency> getCurrencies(Account account, int height, int from, int to) {
        return AccountCurrencyTable.getInstance().getManyBy(new DbClause.LongClause("account_id", account.getId()), height, from, to);
    }

    @Override
    public long getCurrencyUnits(Account account, long currencyId) {
        return AccountCurrencyTable.getCurrencyUnits(account.getId(), currencyId);
    }

    @Override
    public long getCurrencyUnits(Account account, long currencyId, int height) {
        return AccountCurrencyTable.getCurrencyUnits(account.getId(), currencyId, height);
    }

    @Override
    public long getUnconfirmedCurrencyUnits(Account account, long currencyId) {
        return AccountCurrencyTable.getUnconfirmedCurrencyUnits(account.getId(), currencyId);
    }

    @Override
    public void addToCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(account.getId(), currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Math.addExact(currencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(account.getId(), currencyId, currencyUnits, 0);
        } else {
            accountCurrency.units = currencyUnits;
        }
        save(accountCurrency);
        //accountService.listeners.notify(account, AccountEventType.CURRENCY_BALANCE);
        accountEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(account);
        //currencyListeners.notify(accountCurrency, AccountEventType.CURRENCY_BALANCE);
        accountCurrencyEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(accountCurrency);
        if (AccountLedger.mustLogEntry(account.getId(), false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(), LedgerHolding.CURRENCY_BALANCE, currencyId,
                    units, currencyUnits));
        }
    }

    @Override
    public void addToUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency = AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(account.getId(), currencyId));
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(account.getId(), currencyId, 0, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        save(accountCurrency);
        //accountService.listeners.notify(account, AccountEventType.UNCONFIRMED_CURRENCY_BALANCE);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(account);
        //currencyListeners.notify(accountCurrency, AccountEventType.UNCONFIRMED_CURRENCY_BALANCE);
        accountCurrencyEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(accountCurrency);
        if (AccountLedger.mustLogEntry(account.getId(), true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                    units, unconfirmedCurrencyUnits));
        }
    }

    @Override
    public void addToCurrencyAndUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = AccountCurrencyTable.getInstance().get(AccountCurrencyTable.newKey(account.getId(), currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.units;
        currencyUnits = Math.addExact(currencyUnits, units);
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(account.getId(), currencyId, currencyUnits, unconfirmedCurrencyUnits);
        } else {
            accountCurrency.units = currencyUnits;
            accountCurrency.unconfirmedUnits = unconfirmedCurrencyUnits;
        }
        save(accountCurrency);
        //accountService.listeners.notify(account, AccountEventType.CURRENCY_BALANCE);
        accountEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(account);
        //accountService.listeners.notify(account, AccountEventType.UNCONFIRMED_CURRENCY_BALANCE);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(account);
        //currencyListeners.notify(accountCurrency, AccountEventType.CURRENCY_BALANCE);
        accountCurrencyEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(accountCurrency);
        //currencyListeners.notify(accountCurrency, AccountEventType.UNCONFIRMED_CURRENCY_BALANCE);
        accountCurrencyEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(accountCurrency);
        if (AccountLedger.mustLogEntry(account.getId(), true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                    units, unconfirmedCurrencyUnits));
        }
        if (AccountLedger.mustLogEntry(account.getId(), false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.CURRENCY_BALANCE, currencyId,
                    units, currencyUnits));
        }
    }
}
