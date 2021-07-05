/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;

import java.util.stream.Stream;

public interface AssetTransferService {

    /**
     * @deprecated pls use Stream version instead
     */
    DbIterator<AssetTransfer> getAllTransfers(int from, int to);

    Stream<AssetTransfer> getAllTransfersStream(int from, int to);

    int getCount();

    /**
     * @deprecated pls use Stream version instead
     */
    DbIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to);

    Stream<AssetTransfer> getAssetTransfersStream(long assetId, int from, int to);

    /**
     * @deprecated pls use Stream version instead
     */
    DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to);

    Stream<AssetTransfer> getAccountAssetTransfersStream(long accountId, int from, int to);

    /**
     * @deprecated pls use Stream version instead
     */
    DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to);

    Stream<AssetTransfer> getAccountAssetTransfersStream(long accountId, long assetId, int from, int to);

    int getTransferCount(long assetId);

    AssetTransfer addAssetTransfer(Transaction transaction, ColoredCoinsAssetTransfer attachment);
}
