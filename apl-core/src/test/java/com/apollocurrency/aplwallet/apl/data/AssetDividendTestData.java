/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDividend;

import java.util.List;

public class AssetDividendTestData {

    public final AssetDividend ASSET_DIVIDEND_0 = createAssetDividend(
        1L,     7584440193513719551L,    8076646017490321411L,    1L,          61449,           0,              0,             36559619, 61468);
    public final AssetDividend ASSET_DIVIDEND_1 = createAssetDividend(
        2L,     -7390004979265954310L,   8804302123230545017L,    1000,       61449,           0,              0,             36559807, 61487);
    public final AssetDividend ASSET_DIVIDEND_2 = createAssetDividend(
        3L,     9191632407374355191L,    9065918785929852826L,    1000,       61449,           0,              0,             36560092, 61516);
    public final AssetDividend ASSET_DIVIDEND_3 = createAssetDividend(
        4L,     8033155246743541720L,    9065918785929852826L,    100,        61449,           0,              0,             36564916, 62007);

    public final AssetDividend ASSET_DIVIDEND_NEW = createNewAssetDividend(
        8033100046743541720L,    9065000785929852826L,    100,        61449,           0,              0,             36564916, 62007);

    public List<AssetDividend> ALL_ASSETS_DIVIDEND_ORDERED_BY_DBID = List.of(
        ASSET_DIVIDEND_3, ASSET_DIVIDEND_2, ASSET_DIVIDEND_1, ASSET_DIVIDEND_0);

    public AssetDividend createAssetDividend(long dbId, long id, long assetId, long amount,
                                             int dividendHeight, long totalDividend, long numAccounts, int timestamp, int height) {
        AssetDividend assetDelete = new AssetDividend(id, assetId, amount, dividendHeight, totalDividend, numAccounts, timestamp, height);
        assetDelete.setDbId(dbId);
        return assetDelete;
    }

    public AssetDividend createNewAssetDividend(long id, long assetId, long amount,
                                                int dividendHeight, long totalDividend,
                                                long numAccounts, int timestamp, int height) {
        return new AssetDividend(id, assetId, amount, dividendHeight, totalDividend, numAccounts, timestamp, height);
    }

}
