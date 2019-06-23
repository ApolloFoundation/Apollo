package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.*;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import lombok.Setter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class AccountAssetServiceImpl implements AccountAssetService {

    @Inject @Setter
    private AccountAssetTable accountAssetTable;

    @Inject @Setter
    private AccountService accountService;

    @Override
    public DbIterator<AccountAsset> getAssets(AccountEntity account, int from, int to) {
        return accountAssetTable.getAccountAssets(account.getId(), from, to);
    }

    @Override
    public DbIterator<AccountAsset> getAssets(AccountEntity account, int height, int from, int to) {
        return accountAssetTable.getAccountAssets(account.getId(), height, from, to);
    }

    @Override
    public List<AccountAsset> getAssetAccounts(long assetId, int height) {
        List<AccountAsset> accountAssets = new ArrayList<>();
        try (DbIterator<AccountAsset> iterator = accountAssetTable.getAssetAccounts(assetId, height, 0, -1)) {
            while (iterator.hasNext()) {
                accountAssets.add(iterator.next());
            }
        }
        return accountAssets;
    }

    @Override
    public AccountAsset getAsset(AccountEntity account, long assetId) {
        return accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
    }

    @Override
    public AccountAsset getAsset(AccountEntity account, long assetId, int height) {
        return accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId), height);
    }

    @Override
    public long getAssetBalanceATU(AccountEntity account, long assetId) {
        return AccountAssetTable.getAssetBalanceATU(account.getId(), assetId);
    }

    @Override
    public long getAssetBalanceATU(AccountEntity account, long assetId, int height) {
        return AccountAssetTable.getAssetBalanceATU(account.getId(), assetId, height);
    }

    @Override
    public long getUnconfirmedAssetBalanceATU(AccountEntity account, long assetId) {
        return AccountAssetTable.getUnconfirmedAssetBalanceATU(account.getId(), assetId);
    }

    @Override
    public void addToAssetBalanceATU(AccountEntity accountEntity, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(accountEntity.getId(), assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityATU;
        assetBalance = Math.addExact(assetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(accountEntity.getId(), assetId, assetBalance, 0);
        } else {
            accountAsset.quantityATU = assetBalance;
        }
        AccountAssetTable.getInstance().save(accountAsset);
        accountService.listeners.notify(accountService.getAccount(accountEntity), Account.Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.ASSET_BALANCE);
        if (AccountLedger.mustLogEntry(accountEntity.getId(), false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, accountEntity.getId(), LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance));
        }
    }

    @Override
    public void addToUnconfirmedAssetBalanceATU(AccountEntity accountEntity, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(accountEntity.getId(), assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(accountEntity.getId(), assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.unconfirmedQuantityATU = unconfirmedAssetBalance;
        }
        accountAssetTable.save(accountAsset);
        accountService.listeners.notify(accountService.getAccount(accountEntity), Account.Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.UNCONFIRMED_ASSET_BALANCE);
        if (event == null) {
            return;
        }
        if (AccountLedger.mustLogEntry(accountEntity.getId(), true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, accountEntity.getId(),
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance));
        }
    }

    @Override
    public void addToAssetAndUnconfirmedAssetBalanceATU(AccountEntity accountEntity, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset;
        accountAsset = AccountAssetTable.getInstance().get(AccountAssetTable.newKey(accountEntity.getId(), assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.quantityATU;
        assetBalance = Math.addExact(assetBalance, quantityATU);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.unconfirmedQuantityATU;
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(accountEntity.getId(), assetId, assetBalance, unconfirmedAssetBalance);
        } else {
            accountAsset.quantityATU = assetBalance;
            accountAsset.unconfirmedQuantityATU = unconfirmedAssetBalance;
        }
        AccountAssetTable.getInstance().save(accountAsset);
        accountService.listeners.notify(accountService.getAccount(accountEntity), Account.Event.ASSET_BALANCE);
        accountService.listeners.notify(accountService.getAccount(accountEntity), Account.Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.UNCONFIRMED_ASSET_BALANCE);
        if (AccountLedger.mustLogEntry(accountEntity.getId(), true)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, accountEntity.getId(),
                    LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
                    quantityATU, unconfirmedAssetBalance));
        }
        if (AccountLedger.mustLogEntry(accountEntity.getId(), false)) {
            AccountLedger.logEntry(new LedgerEntry(event, eventId, accountEntity.getId(),
                    LedgerHolding.ASSET_BALANCE, assetId,
                    quantityATU, assetBalance));
        }
    }

}
