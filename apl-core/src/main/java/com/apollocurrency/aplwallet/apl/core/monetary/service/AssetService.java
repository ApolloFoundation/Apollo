/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.service;

import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;

public interface AssetService {

    DbIterator<Asset> getAllAssets(int from, int to);

    Stream<Asset> getAllAssetsStream(int from, int to);

    int getCount();

    Asset getAsset(long id);

    Asset getAsset(long id, int height);

    DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to);

    Stream<Asset> getAssetsIssuedByStream(long accountId, int from, int to);

    DbIterator<Asset> searchAssets(String query, int from, int to);

    Stream<Asset> searchAssetsStream(String query, int from, int to);

    void addAsset(Transaction transaction, ColoredCoinsAssetIssuance attachment);

    void deleteAsset(Transaction transaction, long assetId, long quantityATU);

}
