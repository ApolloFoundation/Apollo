/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.account.model.*;
import com.apollocurrency.aplwallet.apl.core.account.service.*;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Trade;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.Exchange;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Used as factory to produce the Account objects
 */
@Singleton
public final class AccountFactory {

    @Inject @Setter
    private Blockchain blockchain;

    @Inject @Setter
    private BlockchainConfig blockchainConfig;

    @Inject @Setter
    private AccountService accountService;
    @Inject @Setter
    private AccountInfoService accountInfoService;
    @Inject @Setter
    private AccountLeaseService accountLeaseService;
    @Inject @Setter
    private AccountAssetService accountAssetService;
    @Inject @Setter
    private AccountCurrencyService accountCurrencyService;
    @Inject @Setter
    private AccountPropertyService accountPropertyService;
    @Inject @Setter
    private AccountPublickKeyService accountPublickKeyService;

    public Account createAccount(AccountEntity entity){
        if (entity == null)
            return null;

        Account account = new AccountWrapper( entity,
                blockchain, blockchainConfig,
                accountService, accountInfoService,
                accountLeaseService, accountAssetService, accountCurrencyService,
                accountPropertyService, accountPublickKeyService);
        return account;
    }

    private class AccountWrapper implements Account {

        private AccountEntity account;

        private Blockchain blockchain;
        private BlockchainConfig blockchainConfig;

        private AccountService accountService;
        private AccountInfoService accountInfoService;
        private AccountLeaseService accountLeaseService;
        private AccountAssetService accountAssetService;
        private AccountCurrencyService accountCurrencyService;
        private AccountPropertyService accountPropertyService;
        private AccountPublickKeyService accountPublickKeyService;

        AccountWrapper(AccountEntity account,
                       Blockchain blockchain,
                       BlockchainConfig blockchainConfig,
                       AccountService accountService,
                       AccountInfoService accountInfoService,
                       AccountLeaseService accountLeaseService,
                       AccountAssetService accountAssetService,
                       AccountCurrencyService accountCurrencyService,
                       AccountPropertyService accountPropertyService,
                       AccountPublickKeyService accountPublickKeyService) {
            this.account = account;

            this.blockchain = blockchain;
            this.blockchainConfig = blockchainConfig;
            this.accountService = accountService;
            this.accountInfoService = accountInfoService;
            this.accountLeaseService = accountLeaseService;
            this.accountAssetService = accountAssetService;
            this.accountAssetService = accountAssetService;
            this.accountCurrencyService = accountCurrencyService;
            this.accountPropertyService = accountPropertyService;
            this.accountPublickKeyService = accountPublickKeyService;

        }

        @Override
        public Account getAccount(long id) {
            return accountService.getAccount(id);
        }

        @Override
        public AccountEntity getEntity(){
            return account;
        }

        @Override
        public void save() {
            accountService.save(account);
        }

        @Override
        public long getId() {
            return account.getId();
        }
        @Override
        public long getBalanceATM() {
            return account.getBalanceATM();
        }

        @Override
        public long getUnconfirmedBalanceATM() {
            return account.getUnconfirmedBalanceATM();
        }

        @Override
        public long getForgedBalanceATM() {
            return account.getForgedBalanceATM();
        }

        @Override
        public long getActiveLesseeId() {
            return account.getActiveLesseeId();
        }

        @Override
        public Set<ControlType> getControls() {
            return account.getControls();
        }

        @Override
        public boolean addControl(ControlType control) {
            return account.addControl(control);
        }

        @Override
        public boolean removeControl(ControlType control) {
            return account.removeControl(control);
        }

        @Override
        public AccountInfo getAccountInfo() {
            return accountInfoService.getAccountInfo(account);
        }

        @Override
        public void setAccountInfo(String name, String description) {
            accountInfoService.updateAccountInfo(account, name, description);
        }

        @Override
        public AccountLease getAccountLease() {
            return accountLeaseService.getAccountLease(account);
        }

        @Override
        public EncryptedData encryptTo(byte[] data, byte[] keySeed, boolean compress) {
            return accountPublickKeyService.encryptTo(account.getId(), data, keySeed, compress);
        }

        @Override
        public byte[] decryptFrom(EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress) {
            return accountPublickKeyService.decryptFrom(account.getId(), encryptedData, recipientKeySeed, uncompress);
        }

        @Override
        public long getEffectiveBalanceAPL() {
            return getEffectiveBalanceAPL(blockchain.getHeight(), true);
        }

        @Override
        public long getEffectiveBalanceAPL(int height, boolean lock) {
            return accountService.getEffectiveBalanceAPL(account, height, lock);
        }

        @Override
        public long getLessorsGuaranteedBalanceATM(int height) {
            return accountService.getLessorsGuaranteedBalanceATM(account, height);
        }

        @Override
        public DbIterator<AccountEntity> getLessors() {
            return accountService.getLessorsIterator(account);
        }

        @Override
        public DbIterator<AccountEntity> getLessors(int height) {
            return accountService.getLessorsIterator(account, height);
        }

        @Override
        public long getGuaranteedBalanceATM() {
            return getGuaranteedBalanceATM(blockchainConfig.getGuaranteedBalanceConfirmations(), blockchain.getHeight());
        }

        @Override
        public long getGuaranteedBalanceATM(final int numberOfConfirmations, final int currentHeight) {
            return accountService.getGuaranteedBalanceATM(account, numberOfConfirmations, currentHeight);
        }

        @Override
        public DbIterator<AccountAsset> getAssets(int from, int to) {
            return accountAssetService.getAssets(account, from, to);
        }

        @Override
        public DbIterator<AccountAsset> getAssets(int height, int from, int to) {
            return accountAssetService.getAssets(account, height, from, to);
        }

        @Override
        public DbIterator<Trade> getTrades(int from, int to) {
            return Trade.getAccountTrades(account.getId(), from, to);
        }

        @Override
        public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
            return AssetTransfer.getAccountAssetTransfers(account.getId(), from, to);
        }

        @Override
        public DbIterator<CurrencyTransfer> getCurrencyTransfers(int from, int to) {
            return CurrencyTransfer.getAccountCurrencyTransfers(account.getId(), from, to);
        }

        @Override
        public DbIterator<Exchange> getExchanges(int from, int to) {
            return Exchange.getAccountExchanges(account.getId(), from, to);
        }

        @Override
        public AccountAsset getAsset(long assetId) {
            return accountAssetService.getAsset(account, assetId);
        }

        @Override
        public AccountAsset getAsset(long assetId, int height) {
            return accountAssetService.getAsset(account, assetId, height);
        }

        @Override
        public long getAssetBalanceATU(long assetId) {
            return accountAssetService.getAssetBalanceATU(account, assetId);
        }

        @Override
        public long getAssetBalanceATU(long assetId, int height) {
            return accountAssetService.getAssetBalanceATU(account, assetId, height);
        }

        @Override
        public long getUnconfirmedAssetBalanceATU(long assetId) {
            return accountAssetService.getUnconfirmedAssetBalanceATU(account, assetId);
        }

        @Override
        public AccountCurrency getCurrency(long currencyId) {
            return accountCurrencyService.getCurrency(account, currencyId);
        }

        @Override
        public AccountCurrency getCurrency(long currencyId, int height) {
            return accountCurrencyService.getCurrency(account, currencyId, height);
        }

        @Override
        public DbIterator<AccountCurrency> getCurrencies(int from, int to) {
            return accountCurrencyService.getCurrencies(account, from, to);
        }

        @Override
        public DbIterator<AccountCurrency> getCurrencies(int height, int from, int to) {
            return accountCurrencyService.getCurrencies(account, height, from, to);
        }

        @Override
        public long getCurrencyUnits(long currencyId) {
            return accountCurrencyService.getCurrencyUnits(account, currencyId);
        }

        @Override
        public long getCurrencyUnits(long currencyId, int height) {
            return accountCurrencyService.getCurrencyUnits(account, currencyId, height);
        }

        @Override
        public long getUnconfirmedCurrencyUnits(long currencyId) {
            return accountCurrencyService.getUnconfirmedCurrencyUnits(account, currencyId);
        }

        @Override
        public void leaseEffectiveBalance(long lesseeId, int period) {
            accountLeaseService.leaseEffectiveBalance(account, lesseeId, period);
        }

        @Override
        public void setProperty(Transaction transaction, Account setterAccount, String property, String value) {
            accountPropertyService.setProperty(account, transaction, setterAccount.getEntity(), property, value);
        }

        @Override
        public  void deleteProperty(long propertyId) {
            accountPropertyService.deleteProperty(account, propertyId);
        }

        @Override
        public void apply(byte[] key) {
            apply(key, false);
        }

        @Override
        public void apply(byte[] key, boolean isGenesis) {
            accountPublickKeyService.apply(account, key, isGenesis);
        }

        @Override
        public void addToAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
            accountAssetService.addToAssetBalanceATU(account, event, eventId, assetId, quantityATU);
        }

        @Override
        public void addToUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
            accountAssetService.addToUnconfirmedAssetBalanceATU(account, event, assetId, assetId, quantityATU);
        }

        @Override
        public void addToAssetAndUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU) {
            accountAssetService.addToAssetAndUnconfirmedAssetBalanceATU(account, event, eventId, assetId, quantityATU);
        }

        @Override
        public void addToCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
            accountCurrencyService.addToCurrencyUnits(account, event, eventId, currencyId, units);
        }

        @Override
        public void addToUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
            accountCurrencyService.addToUnconfirmedCurrencyUnits(account, event, eventId, currencyId, units);
        }

        @Override
        public void addToCurrencyAndUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units) {
            accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(account, event, eventId, currencyId, units);
        }

        @Override
        public  void addToBalanceATM(LedgerEvent event, long eventId, long amountATM) {
            addToBalanceATM(event, eventId, amountATM, 0);
        }

        @Override
        public void addToBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
            accountService.addToBalanceATM(account, event, eventId, amountATM, feeATM);
        }

        @Override
        public void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM) {
            addToUnconfirmedBalanceATM(event, eventId, amountATM, 0);
        }

        @Override
        public void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
            accountService.addToUnconfirmedBalanceATM(account, event, eventId, amountATM, feeATM);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM) {
            addToBalanceAndUnconfirmedBalanceATM(event, eventId, amountATM, 0);
        }

        @Override
        public void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM) {
            accountService.addToBalanceAndUnconfirmedBalanceATM(account, event, eventId, amountATM, feeATM);
        }

        @Override
        public void addToForgedBalanceATM(long amountATM) {
            if (account.addToForgedBalanceATM(amountATM)){
                save();
            }
        }

        @Override
        public int getHeight() {
            return blockchain.getHeight();
        }

        @Override
        public void payDividends(final long transactionId, ColoredCoinsDividendPayment attachment) {
            accountService.payDividends(account, transactionId, attachment);
        }

    }

}
