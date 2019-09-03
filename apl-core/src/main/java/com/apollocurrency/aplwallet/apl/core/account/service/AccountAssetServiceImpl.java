/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.monetary.service.AssetDividendService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountAssetServiceImpl implements AccountAssetService {

    private Blockchain blockchain;
    private AccountAssetTable accountAssetTable;
    private AccountService accountService;
    private Event<Account> accountEvent;
    private Event<AccountAsset> accountAssetEvent;
    private AccountLedgerService accountLedgerService;
    private AssetDividendService assetDividendService;

    @Inject
    public AccountAssetServiceImpl(Blockchain blockchain, AccountAssetTable accountAssetTable, AccountService accountService, Event<Account> accountEvent, Event<AccountAsset> accountAssetEvent, AccountLedgerService accountLedgerService, AssetDividendService assetDividendService) {
        this.blockchain = blockchain;
        this.accountAssetTable = accountAssetTable;
        this.accountService = accountService;
        this.accountEvent = accountEvent;
        this.accountAssetEvent = accountAssetEvent;
        this.accountLedgerService = accountLedgerService;
        this.assetDividendService = assetDividendService;
    }

    @Override
    public List<AccountAsset> getAssets(long assetId, int height) {
        return getAssets(assetId, height, 0, -1);
    }

    @Override
    public List<AccountAsset> getAssets(long assetId, int height, int from, int to){
        return accountAssetTable.getAssetAccounts(assetId, height, from, to);
    }

    @Override
    public List<AccountAsset> getAssetAccounts(Account account, int from, int to) {
        return accountAssetTable.getAccountAssets(account.getId(), from, to);
    }

    @Override
    public List<AccountAsset> getAssetAccounts(Account account, int height, int from, int to) {
        return getAssetAccounts(account.getId(), height, from, to);
    }

    @Override
    public List<AccountAsset> getAssetAccounts(long accountId, int height, int from, int to) {
        return accountAssetTable.getAccountAssets(accountId, height, from, to);
    }

    @Override
    public int getAssetCount(long assetId) {
        return accountAssetTable.getAssetCount(assetId);
    }

    @Override
    public int getAssetCount(long assetId, int height) {
        return accountAssetTable.getAssetCount(assetId, height);
    }

    @Override
    public int getAccountAssetCount(long accountId) {
        return accountAssetTable.getAccountAssetCount(accountId);
    }

    @Override
    public int getAccountAssetCount(long accountId, int height) {
        return accountAssetTable.getAccountAssetCount(accountId, height);
    }

    @Override
    public AccountAsset getAsset(Account account, long assetId) {
        return accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
    }

    @Override
    public AccountAsset getAsset(Account account, long assetId, int height) {
        return getAsset(account.getId(), assetId, height);
    }

    @Override
    public AccountAsset getAsset(long accountId, long assetId, int height) {
        return accountAssetTable.get(AccountAssetTable.newKey(accountId, assetId), height);
    }

    @Override
    public long getAssetBalanceATU(Account account, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        return accountAsset == null ? 0 : accountAsset.getQuantityATU();
    }

    @Override
    public long getAssetBalanceATU(Account account, long assetId, int height) {
        return  getAssetBalanceATU(account.getId(), assetId, height);
    }

    @Override
    public long getAssetBalanceATU(long accountId, long assetId, int height) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(accountId, assetId), height);
        return accountAsset == null ? 0 : accountAsset.getQuantityATU();
    }

    @Override
    public long getUnconfirmedAssetBalanceATU(Account account, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        return accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityATU();
    }

    @Override
    public void addToAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.getQuantityATU();
        assetBalance = Math.addExact(assetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, assetBalance, 0, blockchain.getHeight());
        } else {
            accountAsset.setQuantityATU(assetBalance);
        }
        update(accountAsset);
        //accountService.listeners.notify(account, AccountEventType.ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(account);
        //assetListeners.notify(accountAsset, AccountEventType.ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(accountAsset);
        if (accountLedgerService.mustLogEntry(account.getId(), false)) {
            accountLedgerService.logEntry(new LedgerEntry(event, eventId, account.getId(), LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance, blockchain.getLastBlock()));
        }
    }

    @Override
    public void addToUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityATU();
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, 0, unconfirmedAssetBalance, blockchain.getHeight());
        } else {
            accountAsset.setUnconfirmedQuantityATU(unconfirmedAssetBalance);
        }
        update(accountAsset);
        //accountService.listeners.notify(account, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(account);
        //assetListeners.notify(accountAsset, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(accountAsset);

        if (event == null) {
            return;
        }
        if (accountLedgerService.mustLogEntry(account.getId(), true)) {
            accountLedgerService.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance, blockchain.getLastBlock()));
        }
    }

    @Override
    public void update(AccountAsset accountAsset) {
        AccountService.checkBalance(accountAsset.getAccountId(), accountAsset.getQuantityATU(), accountAsset.getUnconfirmedQuantityATU());
        if (accountAsset.getQuantityATU() > 0 || accountAsset.getUnconfirmedQuantityATU() > 0) {
            accountAssetTable.insert(accountAsset);
        } else {
            accountAssetTable.delete(accountAsset);
        }
    }

    @Override
    public void addToAssetAndUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.getQuantityATU();
        assetBalance = Math.addExact(assetBalance, quantityATU);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityATU();
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, assetBalance, unconfirmedAssetBalance, blockchain.getHeight());
        } else {
            accountAsset.setQuantityATU(assetBalance);
            accountAsset.setUnconfirmedQuantityATU(unconfirmedAssetBalance);
        }
        update(accountAsset);
        //accountService.listeners.notify(account, AccountEventType.ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(account);
        //accountService.listeners.notify(account, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(account);

        //assetListeners.notify(accountAsset, AccountEventType.ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(accountAsset);
        //assetListeners.notify(accountAsset, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(accountAsset);

        if (accountLedgerService.mustLogEntry(account.getId(), true)) {
            accountLedgerService.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance, blockchain.getLastBlock()));
        }
        if (accountLedgerService.mustLogEntry(account.getId(), false)) {
            accountLedgerService.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance, blockchain.getLastBlock()));
        }
    }

    @Override
    public void payDividends(Account account, final long transactionId, ColoredCoinsDividendPayment attachment) {
        long totalDividend = 0;
        List<AccountAsset> accountAssets = getAssets(attachment.getAssetId(), attachment.getHeight());
        final long amountATMPerATU = attachment.getAmountATMPerATU();
        long numAccounts = 0;
        for (final AccountAsset accountAsset : accountAssets) {
            if (accountAsset.getAccountId() != account.getId() && accountAsset.getQuantityATU() != 0) {
                long dividend = Math.multiplyExact(accountAsset.getQuantityATU(), amountATMPerATU);
                accountService.addToBalanceAndUnconfirmedBalanceATM(accountService.getAccount(accountAsset.getAccountId()), LedgerEvent.ASSET_DIVIDEND_PAYMENT, transactionId, dividend);
                totalDividend += dividend;
                numAccounts += 1;
            }
        }
        accountService.addToBalanceATM(account, LedgerEvent.ASSET_DIVIDEND_PAYMENT, transactionId, -totalDividend);
        assetDividendService.addAssetDividend(transactionId, attachment, totalDividend, numAccounts);
    }
}
