/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;

import java.util.stream.Stream;

public interface AssetService {

    /**
     * @deprecated see Stream<> version instead
     */
    DbIterator<Asset> getAllAssets(int from, int to);

    Stream<Asset> getAllAssetsStream(int from, int to);

    int getCount();

    Asset getAsset(long id);

    Asset getAsset(long id, int height);

    /**
     * @deprecated see Stream<> version instead
     */
    DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);

    Stream<Asset> getAssetsIssuedByStream(long accountId, int from, int to);

    /**
     * @deprecated see Stream<> version instead
     */
    DbIterator<Asset> searchAssets(String query, int from, int to);

    Stream<Asset> searchAssetsStream(String query, int from, int to);

    void addAsset(Transaction transaction, ColoredCoinsAssetIssuance attachment);

    void deleteAsset(Transaction transaction, long assetId, long quantityATU);

}
