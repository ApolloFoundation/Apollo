/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;
import static com.apollocurrency.aplwallet.apl.core.account.service.AccountService.checkBalance;
import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountCurrencyServiceImpl implements AccountCurrencyService {

    private final Blockchain blockchain;
    private final AccountCurrencyTable accountCurrencyTable;
    private final Event<LedgerEntry> logLedgerEvent;
    private final Event<Account> accountEvent;
    private final Event<AccountCurrency> accountCurrencyEvent;

    @Inject
    public AccountCurrencyServiceImpl(Blockchain blockchain, AccountCurrencyTable accountCurrencyTable, Event<LedgerEntry> logLedgerEvent, Event<Account> accountEvent, Event<AccountCurrency> accountCurrencyEvent) {
        this.blockchain = blockchain;
        this.accountCurrencyTable = accountCurrencyTable;
        this.logLedgerEvent = logLedgerEvent;
        this.accountEvent = accountEvent;
        this.accountCurrencyEvent = accountCurrencyEvent;
    }

    @Override
    public void update(AccountCurrency currency) {
        checkBalance(currency.getAccountId(), currency.getUnits(), currency.getUnconfirmedUnits());
        if (currency.getUnits() > 0 || currency.getUnconfirmedUnits() > 0) {
            accountCurrencyTable.insert(currency);
        } else if (currency.getUnits() == 0 && currency.getUnconfirmedUnits() == 0) {
            accountCurrencyTable.delete(currency);
        }
    }

    @Override
    public AccountCurrency getAccountCurrency(Account account, long currencyId) {
        return getAccountCurrency(account.getId(), currencyId);
    }

    @Override
    public AccountCurrency getAccountCurrency(long accountId, long currencyId) {
        return accountCurrencyTable.get(AccountCurrencyTable.newKey(accountId, currencyId));
    }

    @Override
    public AccountCurrency getAccountCurrency(Account account, long currencyId, int height) {
        return getAccountCurrency(account.getId(), currencyId, height);
    }

    @Override
    public AccountCurrency getAccountCurrency(long accountId, long currencyId, int height) {
        return accountCurrencyTable.get(AccountCurrencyTable.newKey(accountId, currencyId), height);
    }

    @Override
    public int getCurrencyAccountCount(long currencyId) {
        return accountCurrencyTable.getCurrencyAccountCount(currencyId);
    }

    @Override
    public int getCurrencyAccountCount(long currencyId, int height) {
        return accountCurrencyTable.getCurrencyAccountCount(currencyId, height);
    }

    @Override
    public int getAccountCurrencyCount(long accountId) {
        return accountCurrencyTable.getAccountCurrencyCount(accountId);
    }

    @Override
    public int getAccountCurrencyCount(long accountId, int height) {
        return accountCurrencyTable.getAccountCurrencyCount(accountId, height);
    }

    @Override
    public List<AccountCurrency> getCurrencies(Account account) {
        return getCurrencies(account, 0, -1);
    }

    @Override
    public List<AccountCurrency> getCurrencies(Account account, int from, int to) {
        return getCurrencies(account.getId(), from, to);
    }

    @Override
    public List<AccountCurrency> getCurrencies(long accountId, int from, int to) {
        return toList(accountCurrencyTable.getAccountCurrencies(accountId, from, to));
    }

    @Override
    public List<AccountCurrency> getCurrencies(Account account, int height, int from, int to) {
        return getCurrencies(account.getId(), height, from, to);
    }

    @Override
    public List<AccountCurrency> getCurrencies(long accountId, int height, int from, int to) {
        return toList(accountCurrencyTable.getAccountCurrencies(accountId, height, from, to));
    }

    @Override
    public List<AccountCurrency> getCurrencyAccounts(long currencyId, int from, int to) {
        return toList(accountCurrencyTable.getCurrencyAccounts(currencyId, from, to));
    }

    @Override
    public List<AccountCurrency> getCurrencyAccounts(long currencyId, int height, int from, int to) {
        return toList(accountCurrencyTable.getCurrencyAccounts(currencyId, height, from, to));
    }

    @Override
    public long getCurrencyUnits(Account account, long currencyId) {
        return getCurrencyUnits(account.getId(), currencyId);
    }

    @Override
    public long getCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrencyTable.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.getUnits();
    }

    @Override
    public long getCurrencyUnits(Account account, long currencyId, int height) {
        return getCurrencyUnits(account.getId(), currencyId, height);
    }
    @Override
    public long getCurrencyUnits(long accountId, long currencyId, int height) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrencyTable.newKey(accountId, currencyId), height);
        return accountCurrency == null ? 0 : accountCurrency.getUnits();
    }

    @Override
    public long getUnconfirmedCurrencyUnits(Account account, long currencyId) {
        return getUnconfirmedCurrencyUnits(account.getId(), currencyId);
    }

    @Override
    public long getUnconfirmedCurrencyUnits(long accountId, long currencyId) {
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrencyTable.newKey(accountId, currencyId));
        return accountCurrency == null ? 0 : accountCurrency.getUnconfirmedUnits();
    }

    @Override
    public void addToCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(AccountCurrencyTable.newKey(account.getId(), currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.getUnits();
        currencyUnits = Math.addExact(currencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(account.getId(), currencyId, currencyUnits, 0, blockchain.getHeight());
        } else {
            accountCurrency.setUnits(currencyUnits);
            accountCurrency.setHeight(blockchain.getHeight());
        }
        update(accountCurrency);
        accountEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(account);
        accountCurrencyEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(accountCurrency);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.CURRENCY_BALANCE, currencyId,
                units, currencyUnits, blockchain.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY)).fire(entry);
    }

    @Override
    public void addToUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency = accountCurrencyTable.get(AccountCurrencyTable.newKey(account.getId(), currencyId));
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.getUnconfirmedUnits();
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(account.getId(), currencyId, 0, unconfirmedCurrencyUnits, blockchain.getHeight());
        } else {
            accountCurrency.setUnconfirmedUnits(unconfirmedCurrencyUnits);
            accountCurrency.setHeight(blockchain.getHeight());
        }
        update(accountCurrency);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(account);
        accountCurrencyEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(accountCurrency);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                units, unconfirmedCurrencyUnits, blockchain.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY)).fire(entry);
    }

    @Override
    public void addToCurrencyAndUnconfirmedCurrencyUnits(Account account, LedgerEvent event, long eventId, long currencyId, long units) {
        if (units == 0) {
            return;
        }
        AccountCurrency accountCurrency;
        accountCurrency = accountCurrencyTable.get(AccountCurrencyTable.newKey(account.getId(), currencyId));
        long currencyUnits = accountCurrency == null ? 0 : accountCurrency.getUnits();
        currencyUnits = Math.addExact(currencyUnits, units);
        long unconfirmedCurrencyUnits = accountCurrency == null ? 0 : accountCurrency.getUnconfirmedUnits();
        unconfirmedCurrencyUnits = Math.addExact(unconfirmedCurrencyUnits, units);
        if (accountCurrency == null) {
            accountCurrency = new AccountCurrency(account.getId(), currencyId, currencyUnits, unconfirmedCurrencyUnits, blockchain.getHeight());
        } else {
            accountCurrency.setUnits(currencyUnits);
            accountCurrency.setUnconfirmedUnits(unconfirmedCurrencyUnits);
            accountCurrency.setHeight(blockchain.getHeight());
        }
        update(accountCurrency);
        accountEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(account);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(account);
        accountCurrencyEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(accountCurrency);
        accountCurrencyEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(accountCurrency);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
                units, unconfirmedCurrencyUnits, blockchain.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY)).fire(entry);

        entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.CURRENCY_BALANCE, currencyId,
                units, currencyUnits, blockchain.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY)).fire(entry);
    }
}
