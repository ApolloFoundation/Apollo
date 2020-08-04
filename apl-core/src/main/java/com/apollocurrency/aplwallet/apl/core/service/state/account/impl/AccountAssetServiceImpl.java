/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account.impl;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountAssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDividendService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountEventBinding.literal;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class AccountAssetServiceImpl implements AccountAssetService {

    private final AccountAssetTable accountAssetTable;
    private final AccountService accountService;
    private final Event<Account> accountEvent;
    private final Event<AccountAsset> accountAssetEvent;
    private final Event<LedgerEntry> logLedgerEvent;
    private final AssetDividendService assetDividendService;
    private final BlockChainInfoService blockChainInfoService;

    @Inject
    public AccountAssetServiceImpl(
        AccountAssetTable accountAssetTable,
        AccountService accountService,
        Event<Account> accountEvent,
        Event<AccountAsset> accountAssetEvent,
        Event<LedgerEntry> logLedgerEvent,
        AssetDividendService assetDividendService,
        BlockChainInfoService blockChainInfoService
    ) {
        this.accountAssetTable = accountAssetTable;
        this.accountService = accountService;
        this.accountEvent = accountEvent;
        this.accountAssetEvent = accountAssetEvent;
        this.logLedgerEvent = logLedgerEvent;
        this.assetDividendService = assetDividendService;
        this.blockChainInfoService = blockChainInfoService;
    }

    @Override
    public List<AccountAsset> getAssetsByAssetId(long assetId, int height) {
        return getAssetsByAssetId(assetId, height, 0, -1);
    }

    @Override
    public List<AccountAsset> getAssetsByAssetId(long assetId, int height, int from, int to) {
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return accountAssetTable.getByAssetId(assetId, from, to);
        }
        checkAvailable(height);

        return accountAssetTable.getByAssetId(assetId, height, from, to);
    }

    @Override
    public List<AccountAsset> getAssetsByAccount(Account account, int from, int to) {
        return accountAssetTable.getByAccountId(account.getId(), from, to);
    }

    @Override
    public List<AccountAsset> getAssetsByAccount(Account account, int height, int from, int to) {
        return getAssetsByAccount(account.getId(), height, from, to);
    }

    @Override
    public List<AccountAsset> getAssetsByAccount(long accountId, int height, int from, int to) {
        if (height < 0) {
            return accountAssetTable.getByAccountId(accountId, from, to);
        }
        checkAvailable(height);
        return accountAssetTable.getByAccountId(accountId, height, from, to);
    }

    @Override
    public int getCountByAsset(long assetId) {
        return accountAssetTable.getCountByAssetId(assetId);
    }

    @Override
    public int getCountByAsset(long assetId, int height) {
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return accountAssetTable.getCountByAssetId(assetId);
        }
        checkAvailable(height);
        return accountAssetTable.getCountByAssetId(assetId, height);
    }

    @Override
    public int getCountByAccount(long accountId) {
        return accountAssetTable.getCountByAccountId(accountId);
    }

    @Override
    public int getCountByAccount(long accountId, int height) {
        if (height < 0) {
            return accountAssetTable.getCountByAccountId(accountId);
        }

        checkAvailable(height);
        return accountAssetTable.getCountByAccountId(accountId, height);
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
        final DbKey dbKey = AccountAssetTable.newKey(accountId, assetId);
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return accountAssetTable.get(dbKey);
        }
        checkAvailable(height);
        return accountAssetTable.get(dbKey, height);
    }

    void checkAvailable(int height) {
        if (height + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK < blockChainInfoService.getMinRollbackHeight()) {
            throw new IllegalArgumentException("Historical data as of height " + height + " not available.");
        }
        if (height > blockChainInfoService.getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + blockChainInfoService.getHeight());
        }
    }

    @Override
    public long getAssetBalanceATU(Account account, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        return accountAsset == null ? 0 : accountAsset.getQuantityATU();
    }

    @Override
    public long getAssetBalanceATU(Account account, long assetId, int height) {
        return getAssetBalanceATU(account.getId(), assetId, height);
    }

    @Override
    public long getAssetBalanceATU(long accountId, long assetId, int height) {
        AccountAsset accountAsset = getAsset(accountId, assetId, height);
        return accountAsset == null ? 0 : accountAsset.getQuantityATU();
    }

    @Override
    public long getUnconfirmedAssetBalanceATU(Account account, long assetId) {
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        return accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityATU();
    }

    @Override
    public void addToAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        log.trace(">> addToAssetBalanceATU(..), account={}, event={}, eventId={}, assetId={}, quantityATU={}",
            account, event, eventId, assetId, quantityATU);
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        long assetBalance = accountAsset == null ? 0 : accountAsset.getQuantityATU();
        assetBalance = Math.addExact(assetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, assetBalance, 0, blockChainInfoService.getHeight());
        } else {
            accountAsset.setQuantityATU(assetBalance);
            accountAsset.setHeight(blockChainInfoService.getHeight());
        }
        update(accountAsset);
        accountEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(account);
        accountAssetEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(accountAsset);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.ASSET_BALANCE, assetId,
            quantityATU, assetBalance, blockChainInfoService.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY)).fire(entry);
    }

    @Override
    public void addToUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        log.trace(">> addToUnconfirmedAssetBalanceATU(..), account={}, event={}, eventId={}, assetId={}, quantityATU={}",
            account, event, eventId, assetId, quantityATU);
        if (quantityATU == 0) {
            return;
        }
        AccountAsset accountAsset = accountAssetTable.get(AccountAssetTable.newKey(account.getId(), assetId));
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityATU();
        unconfirmedAssetBalance = Math.addExact(unconfirmedAssetBalance, quantityATU);
        if (accountAsset == null) {
            accountAsset = new AccountAsset(account.getId(), assetId, 0, unconfirmedAssetBalance, blockChainInfoService.getHeight());
        } else {
            accountAsset.setUnconfirmedQuantityATU(unconfirmedAssetBalance);
            accountAsset.setHeight(blockChainInfoService.getHeight());
        }
        update(accountAsset);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(account);
        accountAssetEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(accountAsset);

        if (event == null) {
            return;
        }
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
            quantityATU, unconfirmedAssetBalance, blockChainInfoService.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY)).fire(entry);
    }

    @Override
    public void update(AccountAsset accountAsset) {
        log.trace(">> update() accountAsset = {}", accountAsset);
        AccountService.checkBalance(accountAsset.getAccountId(), accountAsset.getQuantityATU(), accountAsset.getUnconfirmedQuantityATU());
        if (accountAsset.getQuantityATU() > 0 || accountAsset.getUnconfirmedQuantityATU() > 0) {
            accountAssetTable.insert(accountAsset);
            log.trace("<< update() INSERT accountAsset = {}", accountAsset);
        } else {
            int height = blockChainInfoService.getHeight();
            //NOTE in case of issues: accountAsset.setHeight(height);
            accountAssetTable.deleteAtHeight(accountAsset, height);
            log.trace("<< update() DELETE, height={}, accountAsset = {}", height, accountAsset);
        }
    }

    @Override
    public void addToAssetAndUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU) {
        log.trace(">> addToAssetAndUnconfirmedAssetBalanceATU(..), account={}, event={}, eventId={}, assetId={}, quantityATU={}",
            account, event, eventId, assetId, quantityATU);
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
            accountAsset = new AccountAsset(account.getId(), assetId, assetBalance, unconfirmedAssetBalance, blockChainInfoService.getHeight());
        } else {
            accountAsset.setQuantityATU(assetBalance);
            accountAsset.setUnconfirmedQuantityATU(unconfirmedAssetBalance);
            accountAsset.setHeight(blockChainInfoService.getHeight());
        }
        update(accountAsset);
        accountEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(account);
        accountEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(account);

        accountAssetEvent.select(literal(AccountEventType.ASSET_BALANCE)).fire(accountAsset);
        accountAssetEvent.select(literal(AccountEventType.UNCONFIRMED_ASSET_BALANCE)).fire(accountAsset);
        LedgerEntry entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.UNCONFIRMED_ASSET_BALANCE, assetId,
            quantityATU, unconfirmedAssetBalance, blockChainInfoService.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_UNCONFIRMED_ENTRY)).fire(entry);

        entry = new LedgerEntry(event, eventId, account.getId(), LedgerHolding.ASSET_BALANCE, assetId,
            quantityATU, assetBalance, blockChainInfoService.getLastBlock());
        logLedgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.LOG_ENTRY)).fire(entry);
    }

    @Override
    public void payDividends(Account account, final long transactionId, ColoredCoinsDividendPayment attachment) {
        log.trace(">> payDividends(..), account={}, transactionId={}, attachment={}",
            account, transactionId, attachment);
        long totalDividend = 0;
        List<AccountAsset> accountAssets = getAssetsByAssetId(attachment.getAssetId(), attachment.getHeight());
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
