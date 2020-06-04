/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDelete;

import java.util.List;

public class AssetDeleteTestData {

    public AccountTestData accountTestData = new AccountTestData();

    public final AssetDelete ASSET_DELETE_0 = createAssetDelete(
        1L,         3444674909301056677L,    -1072880289966859852L,           accountTestData.ACC_1.getId(),            5,          45690782,   12);
    public final AssetDelete ASSET_DELETE_1 = createAssetDelete(
        2L,         2402544248051582903L,    -1698552298114458330L,           accountTestData.ACC_1.getId(),            10,         45690782,   32);
    public final AssetDelete ASSET_DELETE_2 = createAssetDelete(
        3L,         5373370077664349170L,    -174530643920308495L,            accountTestData.ACC_1.getId(),            10,         45712001,   42);
    public final AssetDelete ASSET_DELETE_3 = createAssetDelete(
        4L,         -780794814210884355L,    -8180990979457659735L,           accountTestData.ACC_2.getId(),            5,          45712647,   55);
    public final AssetDelete ASSET_DELETE_4 = createAssetDelete(
        5L,         -9128485677221760321L,   -7411869947092956999L,           accountTestData.ACC_2.getId(),            10,         45712817,   66);
    public final AssetDelete ASSET_DELETE_5 = createAssetDelete(
        6L,         3746857886535243786L,    -2591338258392940629L,           accountTestData.ACC_5.getId(),            5,          45712884,   81);
    public final AssetDelete ASSET_DELETE_6 = createAssetDelete(
        7L,         5471926494854938613L,    1272486048634857248L,            accountTestData.ACC_5.getId(),            12,         45712896,   94);
    public final AssetDelete ASSET_DELETE_7 = createAssetDelete(
        8L,         2083198303623116770L,    -7671470345148527248L,           accountTestData.ACC_5.getId(),            16,         45712907,   103);

    public final AssetDelete ASSET_DELETE_NEW = createNewAssetDelete(
        2083198303623116888L, -7671470345148527000L,   accountTestData.ACC_5.getId(), 16,45712907, 103);

    public List<AssetDelete> ALL_ASSETS_DELETE_ORDERED_BY_DBID = List.of(
        ASSET_DELETE_7, ASSET_DELETE_6, ASSET_DELETE_5, ASSET_DELETE_4, ASSET_DELETE_3, ASSET_DELETE_2, ASSET_DELETE_1, ASSET_DELETE_0);

    public AssetDelete createAssetDelete(long dbId, long id, long assetId, long senderAccountId,
                                         long quantityATU, int timestamp, int height) {
        AssetDelete assetDelete = new AssetDelete(id, assetId, senderAccountId, quantityATU, timestamp, height);
        assetDelete.setDbId(dbId);
        return assetDelete;
    }

    public AssetDelete createNewAssetDelete(long id, long assetId, long senderAccountId,
                                            long quantityATU, int timestamp, int height) {
        return new AssetDelete(id, assetId, senderAccountId, quantityATU, timestamp, height);
    }

}
