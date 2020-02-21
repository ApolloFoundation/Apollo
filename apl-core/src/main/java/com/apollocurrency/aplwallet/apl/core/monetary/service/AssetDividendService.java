/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.service;

import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsDividendPayment;

import java.util.List;

public interface AssetDividendService {

    List<AssetDividend> getAssetDividends(long assetId, int from, int to);

    AssetDividend getLastDividend(long assetId);

    AssetDividend addAssetDividend(long transactionId, ColoredCoinsDividendPayment attachment, long totalDividend, long numAccounts);

}
