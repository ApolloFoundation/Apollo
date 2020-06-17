/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;

public class AssetTransferTestData {

    public final AssetTransfer ASSET_TRANSFER_0 = createAssetTransfer(
        1,     4942438707784864588L,    8180990979457659735L,    9211698109297098287L,    9211698109297098287L,    1,          33613556, 389);
    public final AssetTransfer ASSET_TRANSFER_1 = createAssetTransfer(
        2,     -2439044672016971496L,   -7671470345148527248L,   9211698109297098287L,    9211698109297098287L,    1,          34842058, 20924);
    public final AssetTransfer ASSET_TRANSFER_2 = createAssetTransfer(
        3,     4634268058494636461L,    -7671470345148527248L,   9211698109297098287L,    9211698109297098287L,    1,          34842598, 20985);
    public final AssetTransfer ASSET_TRANSFER_3 = createAssetTransfer(
        4,     -5780986635613545285L,   -7671470345148527248L,   9211698109297098287L,    9211698109297098287L,    1,          35428343, 28704);
    public final AssetTransfer ASSET_TRANSFER_4 = createAssetTransfer(
        5,     -8778723495575557995L,   -7671470345148527248L,   9211698109297098287L,    9211698109297098287L,    1,          35518772, 28754);
    public final AssetTransfer ASSET_TRANSFER_5 = createAssetTransfer(
        6,     5368659037481959260L,    -9067189682550466717L,   -208393164898941117L,    -7316102710792408068L,   50,         37168105, 124462);
    public final AssetTransfer ASSET_TRANSFER_6 = createAssetTransfer(
        7,     -6470395550179438354L,   4296352812771122443L,    -7316102710792408068L,   -208393164898941117L,    50,         37168145, 124466);
    public final AssetTransfer ASSET_TRANSFER_7 = createAssetTransfer(
        8,     6522929850872597192L,    4576453031147051032L,    -208393164898941117L,    -7316102710792408068L,   50,         37168725, 124524);
    public final AssetTransfer ASSET_TRANSFER_8 = createAssetTransfer(
        9,     -1727926599278750726L,   1059195892779923564L,    -7316102710792408068L,   -208393164898941117L,    50,         37168765, 124528);

    public final AssetTransfer ASSET_TRANSFER_NEW = createNewAssetTransfer(
        2083198303623116888L, -7671470345148527000L,   -208393164898941117L, -208393164898941117L, 16,37168790, 134528);

    public List<AssetTransfer> ALL_ASSETS_TRANSFER_ORDERED_BY_DBID = List.of(
        ASSET_TRANSFER_8, ASSET_TRANSFER_7, ASSET_TRANSFER_6, ASSET_TRANSFER_5, ASSET_TRANSFER_4, ASSET_TRANSFER_3, ASSET_TRANSFER_2, ASSET_TRANSFER_1, ASSET_TRANSFER_0);

    public AssetTransfer createAssetTransfer(long dbId, long id, long assetId, long senderAccountId,
                                             long recipientAccountId, int quantity, int timestamp, int height) {
        AssetTransfer assetDelete = new AssetTransfer(id, assetId, senderAccountId, recipientAccountId, quantity, timestamp, height);
        assetDelete.setDbId(dbId);
        return assetDelete;
    }

    public AssetTransfer createNewAssetTransfer(long id, long assetId, long senderAccountId,
                                                long recipientAccountId, int quantity, int timestamp, int height) {
        return new AssetTransfer(id, assetId, senderAccountId, recipientAccountId, quantity, timestamp, height);
    }

}
