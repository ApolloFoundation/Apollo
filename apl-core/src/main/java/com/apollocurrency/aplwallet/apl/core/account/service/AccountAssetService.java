/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountAssetService {

    List<AccountAsset> getAssetAccounts(Account account, int from, int to);

    List<AccountAsset> getAssetAccounts(Account account, int height, int from, int to);

    List<AccountAsset> getAssetAccounts(long accountId, int height, int from, int to);

    int getAssetCount(long assetId);

    int getAssetCount(long assetId, int height);

    List<AccountAsset> getAssets(long assetId, int height, int from, int to);

    List<AccountAsset> getAssets(long assetId, int height);

    int getAccountAssetCount(long accountId);

    int getAccountAssetCount(long accountId, int height);

    AccountAsset getAsset(Account account, long assetId);

    AccountAsset getAsset(Account account, long assetId, int height);

    AccountAsset getAsset(long accountId, long assetId, int height);

    long getAssetBalanceATU(Account account, long assetId);

    long getAssetBalanceATU(Account account, long assetId, int height);

    long getAssetBalanceATU(long accountId, long assetId, int height);

    long getUnconfirmedAssetBalanceATU(Account account, long assetId);

    void addToAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToAssetAndUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void payDividends(Account account, long transactionId, ColoredCoinsDividendPayment attachment);
}
