/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.*;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import lombok.Setter;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.account.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountAssetServiceImpl implements AccountAssetService {

    @Inject @Setter
    private AccountAssetTable accountAssetTable;

    @Inject @Setter
    private AccountService accountService;

    @Inject @Setter
    private Event<Account> accountEvent;

    @Inject @Setter
    private Event<AccountAsset> accountAssetEvent;

    @Override
    public DbIterator<AccountAsset> getAssets(Account account, int from, int to) {
        return accountAssetTable.getAccountAssets(account.getId(), from, to);
    }

    @Override
    public DbIterator<AccountAsset> getAssets(Account account, int height, int from, int to) {
        return accountAssetTable.getAccountAssets(account.getId(), height, from, to);
    }

    @Override
    public List<AccountAsset> getAssetAccounts(long assetId, int height) {
        List<AccountAsset> accountAssets = new ArrayList<>();
        try (DbIterator<AccountAsset> iterator = accountAssetTable.getAssetAccounts(assetId, height, 0, -1)) {
            iterator.forEachRemaining(accountAssets::add);
        }
        return accountAssets;
    }

    @Override
    public AccountAsset getAsset(Account account, long assetId) {
        return accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
    }

    @Override
    public AccountAsset getAsset(Account account, long assetId, int height) {
        return accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId), height);
    }

    @Override
    public long getAssetBalanceATU(Account account, long assetId) {
        return AccountAssetTable.getAssetBalanceATU(account.getId(), assetId);
    }

    @Override
    public long getAssetBalanceATU(Account account, long assetId, int height) {
        return AccountAssetTable.getAssetBalanceATU(account.getId(), assetId, height);
    }

    @Override
    public long getUnconfirmedAssetBalanceATU(Account account, long assetId) {
        return AccountAssetTable.getUnconfirmedAssetBalanceATU(account.getId(), assetId);
    }

    @Override
    public void addToAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(account.getId(), assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityATU;
        assetBalance = Math.addExact(assetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, assetBalance, 0);
        } else {
            accountAsset.quantityATU = assetBalance;
        }
        AccountAssetTable.getInstance().save(accountAsset);
        //accountService.listeners.notify(account, AccountEventType.ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(account);
        //assetListeners.notify(accountAsset, AccountEventType.ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(accountAsset);
        if (AccountLedger.mustLogEntry(account.getId(), false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(), LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance));
        }
    }

    @Override
    public void addToUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(account.getId(), assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.unconfirmedQuantityATU = unconfirmedAssetBalance;
        }
        accountAssetTable.save(accountAsset);
        //accountService.listeners.notify(account, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(account);
        //assetListeners.notify(accountAsset, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(accountAsset);

        if (event == null) {
            return;
        }
        if (AccountLedger.mustLogEntry(account.getId(), true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance));
        }
    }

    @Override
    public void addToAssetAndUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(account.getId(), assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityATU;
        assetBalance = Math.addExact(assetBalance, quantityATU);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, assetBalance, unconfirmedAssetBalance);
        } else {
            accountAsset.quantityATU = assetBalance;
            accountAsset.unconfirmedQuantityATU = unconfirmedAssetBalance;
        }
        AccountAssetTable.getInstance().save(accountAsset);
        //accountService.listeners.notify(account, AccountEventType.ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(account);
        //accountService.listeners.notify(account, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(account);

        //assetListeners.notify(accountAsset, AccountEventType.ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(accountAsset);
        //assetListeners.notify(accountAsset, AccountEventType.UNCONFIRMED_ASSET_BALANCE);
        accountAssetEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(accountAsset);

        if (AccountLedger.mustLogEntry(account.getId(), true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance));
        }
        if (AccountLedger.mustLogEntry(account.getId(), false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, account.getId(),
                    LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance));
        }
    }

    @Override
    public void payDividends(Account account, final long transactionId, ColoredCoinsDividendPayment attachment) {
        long totalDividend = 0;
        List<AccountAsset> accountAssets = getAssetAccounts(attachment.getAssetId(), attachment.getHeight());
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
        AssetDividend.addAssetDividend(transactionId, attachment, totalDividend, numAccounts);
    }

}
