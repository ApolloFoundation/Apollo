/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;

public class AssetData {

    public AccountTestData accountTestData = new AccountTestData();

    public final Asset ASSET_0 = createAsset(
        1L, -1072880289966859852L,   accountTestData.ACC_1.getId(), "Assets1.1", "ThisisSecretCoin1.1", 10, (byte)1, 10, true);
    public final Asset ASSET_1 = createAsset(
        3L, -1698552298114458330L,   accountTestData.ACC_1.getId(), "Assets1.2", "ThisisSecretCoin1.2", 20, (byte)3, 30, true);
    public final Asset ASSET_2 = createAsset(
        4L, -174530643920308495L,   accountTestData.ACC_1.getId(), "Assets1.3", "ThisisSecretCoin1.3", 30, (byte)4, 40, true);
    public final Asset ASSET_3 = createAsset(
        5L, 8180990979457659735L,   accountTestData.ACC_2.getId(), "Assets2.1", "ThisisSecretCoin2.1", 10, (byte)5, 50, true);
    public final Asset ASSET_4 = createAsset(
        6L, -7411869947092956999L,   accountTestData.ACC_2.getId(), "Assets2.2", "ThisisSecretCoin2.2", 20, (byte)6, 60, true);
    public final Asset ASSET_5 = createAsset(
        8L, -2591338258392940629L,   accountTestData.ACC_5.getId(), "Assets3.1", "ThisisSecretCoin3.1", 10, (byte)8, 80, true);
    public final Asset ASSET_6 = createAsset(
        9L, 1272486048634857248L,   accountTestData.ACC_5.getId(), "Assets3.2", "ThisisSecretCoin3.2", 20, (byte)9, 90, true);
    public final Asset ASSET_7 = createAsset(
        10L, -7671470345148527248L,   accountTestData.ACC_5.getId(), "Assets3.3", "ThisisSecretCoin3.3", 30, (byte)10, 100, true);

    public final Asset ASSET_NEW = createNewAsset(
        -6771470345148327548L,   accountTestData.ACC_5.getId(), "Assets3.4", "ThisisSecretCoin3.4", 40, (byte)11, 110, true);

    public List<Asset> ALL_ASSETS_ORDERED_BY_ID = List.of(ASSET_7, ASSET_4, ASSET_5, ASSET_1, ASSET_0, ASSET_2, ASSET_6, ASSET_3);

    public Asset createAsset(long dbId, long assetId, long senderAccountId, String name, String description,
                             long quantityATU, byte decimals, int height, boolean latest) {
        Asset asset = new Asset(assetId, senderAccountId, new ColoredCoinsAssetIssuance(
            name, description, quantityATU, decimals), height);
        asset.setDbId(dbId);
        asset.setLatest(latest);
        return asset;
    }

    public Asset createNewAsset(long assetId, long senderAccountId, String name, String description,
                             long quantityATU, byte decimals, int height, boolean latest) {
        Asset asset = new Asset(assetId, senderAccountId, new ColoredCoinsAssetIssuance(
            name, description, quantityATU, decimals), height);
        asset.setLatest(latest);
        return asset;
    }

}
