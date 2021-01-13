/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountAssetService {

    List<AccountAsset> getAssetsByAccount(Account account, int from, int to);

    List<AccountAsset> getAssetsByAccount(Account account, int height, int from, int to);

    List<AccountAsset> getAssetsByAccount(long accountId, int height, int from, int to);

    int getCountByAsset(long assetId);

    int getCountByAsset(long assetId, int height);

    List<AccountAsset> getAssetsByAssetId(long assetId, int height, int from, int to);

    List<AccountAsset> getAssetsByAssetId(long assetId, int height);

    int getCountByAccount(long accountId);

    int getCountByAccount(long accountId, int height);

    AccountAsset getAsset(Account account, long assetId);

    AccountAsset getAsset(Account account, long assetId, int height);

    AccountAsset getAsset(long accountId, long assetId, int height);

    long getAssetBalanceATU(Account account, long assetId);

    long getAssetBalanceATU(Account account, long assetId, int height);

    long getAssetBalanceATU(long accountId, long assetId, int height);

    long getUnconfirmedAssetBalanceATU(Account account, long assetId);

    void addToAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void addToUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void update(AccountAsset accountAsset);

    void addToAssetAndUnconfirmedAssetBalanceATU(Account account, LedgerEvent event, long eventId, long assetId, long quantityATU);

    void payDividends(Account account, long transactionId, ColoredCoinsDividendPayment attachment);
}
