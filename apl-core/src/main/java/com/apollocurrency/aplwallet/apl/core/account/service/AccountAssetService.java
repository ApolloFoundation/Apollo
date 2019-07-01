/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountAssetService {

    DbIterator<AccountAsset> getAssets(Account account, int from, int to);

    DbIterator<AccountAsset> getAssets(Account account, int height, int from, int to);

    List<AccountAsset> getAssetAccounts(Account account);

    List<AccountAsset> getAssetAccounts(long assetId, int height);

    AccountAsset getAsset(Account account, long assetId);

    AccountAsset getAsset(Account account, long assetId, int height);

    long getAssetBalanceATU(Account account, long assetId);

    long getAssetBalanceATU(Account account, long assetId, int height);

    long getUnconfirmedAssetBalanceATU(Account account, long assetId);

    void addToAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToAssetAndUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void payDividends(Account account, long transactionId, ColoredCoinsDividendPayment attachment);
}
