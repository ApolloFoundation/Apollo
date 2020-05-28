/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;

public class AssetData {

    public final Asset ASSET_1 = createAsset(
        1L, -1072880289966859852L,   100, "Assets1.1", "ThisisSecretCoin1.1", 10, (byte)1, 10, true);


    public Asset createAsset(long dbId, long assetId, long senderAccountId, String name, String description,
                             long quantityATU, byte decimals, int height, boolean latest) {
        Asset asset = new Asset(assetId, senderAccountId, new ColoredCoinsAssetIssuance(
            name, description, quantityATU, decimals), height);
        asset.setDbId(dbId);
        asset.setLatest(latest);
        return asset;
    }

}
