/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.state.asset;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDelete;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import java.util.stream.Stream;

public interface AssetDeleteService {

    /**
     * @deprecated see Stream<> version instead
     */
    DbIterator<AssetDelete> getAssetDeletes(long assetId, int from, int to);

    Stream<AssetDelete> getAssetDeletesStream(long assetId, int from, int to);

    /**
     * @deprecated see Stream<> version instead
     */
    DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, int from, int to);

    Stream<AssetDelete> getAccountAssetDeletesStream(long accountId, int from, int to);

    /**
     * @deprecated see Stream<> version instead
     */
    DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, long assetId, int from, int to);

    Stream<AssetDelete> getAccountAssetDeletesStream(long accountId, long assetId, int from, int to);

    AssetDelete addAssetDelete(Transaction transaction, long assetId, long quantityATU);

}
