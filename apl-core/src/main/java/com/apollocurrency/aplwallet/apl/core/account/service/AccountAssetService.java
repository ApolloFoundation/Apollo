/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountAssetService {

    DbIterator<AccountAsset> getAssets(AccountEntity account, int from, int to);

    DbIterator<AccountAsset> getAssets(AccountEntity account, int height, int from, int to);

    List<AccountAsset> getAssetAccounts(long assetId, int height);

    AccountAsset getAsset(AccountEntity account, long assetId);

    AccountAsset getAsset(AccountEntity account, long assetId, int height);

    long getAssetBalanceATU(AccountEntity account, long assetId);

    long getAssetBalanceATU(AccountEntity account, long assetId, int height);

    long getUnconfirmedAssetBalanceATU(AccountEntity account, long assetId);

    void addToAssetBalanceATU(AccountEntity account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToUnconfirmedAssetBalanceATU(AccountEntity account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToAssetAndUnconfirmedAssetBalanceATU(AccountEntity account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void payDividends(AccountEntity account, long transactionId, ColoredCoinsDividendPayment attachment);
}
