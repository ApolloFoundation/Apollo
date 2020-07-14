/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountCurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventBinding.literal;
import static com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService.checkBalance;
import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountCurrencyServiceImpl implements AccountCurrencyService {

    private final AccountCurrencyTable accountCurrencyTable;
    private final Event<LedgerEntry> logLedgerEvent;
    private final Event<Account> accountEvent;
    private final Event<AccountCurrency> accountCurrencyEvent;
    private final BlockChainInfoService blockChainInfoService;

    @Inject
    public AccountCurrencyServiceImpl(
        AccountCurrencyTable accountCurrencyTable,
        Event<LedgerEntry> logLedgerEvent,
        Event<Account> accountEvent,
        Event<AccountCurrency> accountCurrencyEvent,
        BlockChainInfoService blockChainInfoService
    ) {
        this.accountCurrencyTable = accountCurrencyTable;
        this.logLedgerEvent = logLedgerEvent;
        this.accountEvent = accountEvent;
        this.accountCurrencyEvent = accountCurrencyEvent;
        this.blockChainInfoService = blockChainInfoService;
    }

    @Override
    public void update(AccountCurrency currency) {
        checkBalance(currency.getAccountId(), currency.getUnits(), currency.getUnconfirmedUnits());
        if (currency.getUnits() > 0 || currency.getUnconfirmedUnits() > 0) {
            accountCurrencyTable.insert(currency);
        } else if (currency.getUnits() == 0 && currency.getUnconfirmedUnits() == 0) {
            accountCurrencyTable.deleteAtHeight(currency, blockChainInfoService.getHeight());
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
        final DbKey dbKey = AccountCurrencyTable.newKey(accountId, currencyId);
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return accountCurrencyTable.get(dbKey);
        }
        blockChainInfoService.checkAvailable(height, accountCurrencyTable.isMultiversion());

        return accountCurrencyTable.get(dbKey, height);
    }

    @Override
    public int getCountByCurrency(long currencyId) {
        return accountCurrencyTable.getCountByCurrency(currencyId);
    }

    @Override
    public int getCountByCurrency(long currencyId, int height) {
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return accountCurrencyTable.getCountByCurrency(currencyId, height);
        }
        blockChainInfoService.checkAvailable(height, accountCurrencyTable.isMultiversion());

        return accountCurrencyTable.getCountByCurrency(currencyId, height);
    }

    @Override
    public int getCountByAccount(long accountId) {
        return accountCurrencyTable.getCountByAccount(accountId);
    }

    @Override
    public int getCountByAccount(long accountId, int height) {
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return accountCurrencyTable.getCountByAccount(accountId);
        }
        blockChainInfoService.checkAvailable(height, accountCurrencyTable.isMultiversion());

        return accountCurrencyTable.getCountByAccount(accountId, height);
    }

    @Override
    public List<AccountCurrency> getCurrenciesByAccount(Account account) {
        return getCurrenciesByAccount(account, 0, -1);
    }

    @Override
    public List<AccountCurrency> getCurrenciesByAccount(Account account, int from, int to) {
        return getCurrenciesByAccount(account.getId(), from, to);
    }

    @Override
    public List<AccountCurrency> getCurrenciesByAccount(long accountId, int from, int to) {
        return toList(accountCurrencyTable.getByAccount(accountId, from, to));
    }

    @Override
    public List<AccountCurrency> getCurrenciesByAccount(Account account, int height, int from, int to) {
        return getCurrenciesByAccount(account.getId(), height, from, to);
    }

    @Override
    public List<AccountCurrency> getCurrenciesByAccount(long accountId, int height, int from, int to) {
        if (height < 0) {
            return toList(accountCurrencyTable.getByAccount(accountId, from, to));
        } else {
            return toList(accountCurrencyTable.getByAccount(accountId, height, from, to));
        }
    }

    @Override
    public List<AccountCurrency> getCurrenciesByCurrency(long currencyId, int from, int to) {
        return toList(accountCurrencyTable.getByCurrency(currencyId, from, to));
    }

    @Override
    public List<AccountCurrency> getCurrenciesByCurrency(long currencyId, int height, int from, int to) {
        if (height < 0) {
            return toList(accountCurrencyTable.getByCurrency(currencyId, from, to));
        } else {
            return toList(accountCurrencyTable.getByCurrency(currencyId, height, from, to));
        }
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
        final AccountCurrency accountCurrency = getAccountCurrency(accountId, currencyId, height);
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
            accountCurrency = new AccountCurrency(account.getId(), currencyId, currencyUnits, 0, blockChainInfoService.getHeight());
        } else {
            accountCurrency.setUnits(currencyUnits);
            accountCurrency.setHeight(blockChainInfoService.getHeight());
        }
        update(accountCurrency);
        accountEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(account);
        accountCurrencyEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(accountCurrency);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.CURRENCY_BALANCE, currencyId,
            units, currencyUnits, blockChainInfoService.getLastBlock());
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
            accountCurrency = new AccountCurrency(account.getId(), currencyId, 0, unconfirmedCurrencyUnits, blockChainInfoService.getHeight());
        } else {
            accountCurrency.setUnconfirmedUnits(unconfirmedCurrencyUnits);
            accountCurrency.setHeight(blockChainInfoService.getHeight());
        }
        update(accountCurrency);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(account);
        accountCurrencyEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(accountCurrency);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
            units, unconfirmedCurrencyUnits, blockChainInfoService.getLastBlock());
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
            accountCurrency = new AccountCurrency(account.getId(), currencyId, currencyUnits, unconfirmedCurrencyUnits, blockChainInfoService.getHeight());
        } else {
            accountCurrency.setUnits(currencyUnits);
            accountCurrency.setUnconfirmedUnits(unconfirmedCurrencyUnits);
            accountCurrency.setHeight(blockChainInfoService.getHeight());
        }
        update(accountCurrency);
        accountEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(account);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(account);
        accountCurrencyEvent.select(literal(AccountEventType.CURRENCY_BALANCE)).fire(accountCurrency);
        accountCurrencyEvent.select(literal(AccountEventType.UNCONFIRMED_CURRENCY_BALANCE)).fire(accountCurrency);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.UNCONFIRMED_CURRENCY_BALANCE, currencyId,
            units, unconfirmedCurrencyUnits, blockChainInfoService.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY)).fire(entry);

        entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.CURRENCY_BALANCE, currencyId,
            units, currencyUnits, blockChainInfoService.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY)).fire(entry);
    }
}
