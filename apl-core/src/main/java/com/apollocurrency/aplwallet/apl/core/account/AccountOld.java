package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.account.model.*;
import com.apollocurrency.aplwallet.apl.core.app.Trade;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.monetary.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.monetary.Exchange;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;

import java.util.Set;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Deprecated
public interface AccountOld {

    AccountEntity getEntity();
    //delegated from AccountEntity
    long getId();
    long getBalanceATM();
    long getUnconfirmedBalanceATM();
    long getForgedBalanceATM();
    long getActiveLesseeId();
    Set<AccountControlType> getControls();
    boolean addControl(AccountControlType control);
    boolean removeControl(AccountControlType control);
    void addToForgedBalanceATM(long amountATM);
    //

    int getHeight();

    void save();

    AccountInfo getAccountInfo();

    void setAccountInfo(String name, String description);

    AccountLease getAccountLease();

    EncryptedData encryptTo(byte[] data, byte[] keySeed, boolean compress);

    byte[] decryptFrom(EncryptedData encryptedData, byte[] recipientKeySeed, boolean uncompress);


    long getEffectiveBalanceAPL();

    long getEffectiveBalanceAPL(int height, boolean lock);

    long getLessorsGuaranteedBalanceATM(int height);

    DbIterator<AccountEntity> getLessors();

    DbIterator<AccountEntity> getLessors(int height);

    long getGuaranteedBalanceATM();

    long getGuaranteedBalanceATM(int numberOfConfirmations, int currentHeight);

    DbIterator<AccountAsset> getAssets(int from, int to);

    DbIterator<AccountAsset> getAssets(int height, int from, int to);

    DbIterator<Trade> getTrades(int from, int to);

    DbIterator<AssetTransfer> getAssetTransfers(int from, int to);

    DbIterator<CurrencyTransfer> getCurrencyTransfers(int from, int to);

    DbIterator<Exchange> getExchanges(int from, int to);

    AccountAsset getAsset(long assetId);

    AccountAsset getAsset(long assetId, int height);

    long getAssetBalanceATU(long assetId);

    long getAssetBalanceATU(long assetId, int height);

    long getUnconfirmedAssetBalanceATU(long assetId);

    AccountCurrency getCurrency(long currencyId);

    AccountCurrency getCurrency(long currencyId, int height);

    DbIterator<AccountCurrency> getCurrencies(int from, int to);

    DbIterator<AccountCurrency> getCurrencies(int height, int from, int to);

    long getCurrencyUnits(long currencyId);

    long getCurrencyUnits(long currencyId, int height);

    long getUnconfirmedCurrencyUnits(long currencyId);

    void leaseEffectiveBalance(long lesseeId, int period);

    void setProperty(Transaction transaction, AccountOld setterAccount, String property, String value);

    void deleteProperty(long propertyId);

    void apply(byte[] key);

    void apply(byte[] key, boolean isGenesis);

    void addToAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToAssetAndUnconfirmedAssetBalanceATU(LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units);

    void addToUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units);

    void addToCurrencyAndUnconfirmedCurrencyUnits(LedgerEvent event, long eventId, long currencyId, long units);

    void addToBalanceATM(LedgerEvent event, long eventId, long amountATM);

    void addToBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM);

    void addToUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM);

    void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM);

    void addToBalanceAndUnconfirmedBalanceATM(LedgerEvent event, long eventId, long amountATM, long feeATM);

    void payDividends(long transactionId, ColoredCoinsDividendPayment attachment);
}
