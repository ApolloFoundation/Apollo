/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTransferTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetTransferService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAssetTransferAttachment;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Singleton
public class AssetTransferServiceImpl implements AssetTransferService {

    private final AssetTransferTable assetTransferTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<AssetTransfer> assetIteratorToStreamConverter;

    @Inject
    public AssetTransferServiceImpl(AssetTransferTable assetTransferTable,
                                    BlockChainInfoService blockChainInfoService
    ) {
        this.assetTransferTable = assetTransferTable;
        this.blockChainInfoService = blockChainInfoService;
        this.assetIteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * Constructor for unit tests
     */
    public AssetTransferServiceImpl(AssetTransferTable assetTransferTable,
                                    BlockChainInfoService blockChainInfoService,
                                    IteratorToStreamConverter<AssetTransfer> assetIteratorToStreamConverter
    ) {
        this.assetTransferTable = assetTransferTable;
        this.blockChainInfoService = blockChainInfoService;
        if (assetIteratorToStreamConverter != null) {
            this.assetIteratorToStreamConverter = assetIteratorToStreamConverter;
        } else {
            this.assetIteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
    }

    @Override
    public DbIterator<AssetTransfer> getAllTransfers(int from, int to) {
        return assetTransferTable.getAll(from, to);
    }

    @Override
    public Stream<AssetTransfer> getAllTransfersStream(int from, int to) {
        return assetIteratorToStreamConverter.apply(assetTransferTable.getAll(from, to));
    }

    @Override
    public int getCount() {
        return assetTransferTable.getCount();
    }

    @Override
    public DbIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
        return assetTransferTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    @Override
    public Stream<AssetTransfer> getAssetTransfersStream(long assetId, int from, int to) {
        return assetIteratorToStreamConverter.apply(
            assetTransferTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to));
    }

    @Override
    public DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
        return assetTransferTable.getAccountAssetTransfers(accountId, from, to);
    }

    @Override
    public Stream<AssetTransfer> getAccountAssetTransfersStream(long accountId, int from, int to) {
        return assetIteratorToStreamConverter.apply(assetTransferTable.getAccountAssetTransfers(accountId, from, to));
    }

    @Override
    public DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
        return assetTransferTable.getAccountAssetTransfers(accountId, assetId, from, to);
    }

    @Override
    public Stream<AssetTransfer> getAccountAssetTransfersStream(long accountId, long assetId, int from, int to) {
        return assetIteratorToStreamConverter.apply(assetTransferTable.getAccountAssetTransfers(accountId, assetId, from, to));
    }

    @Override
    public int getTransferCount(long assetId) {
        return assetTransferTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    @Override
    public AssetTransfer addAssetTransfer(Transaction transaction, CCAssetTransferAttachment attachment) {
        Block lastBlock = blockChainInfoService.getLastBlock();
        AssetTransfer assetTransfer = new AssetTransfer(transaction, attachment,
            lastBlock.getTimestamp(), lastBlock.getHeight());
        assetTransferTable.insert(assetTransfer);
        return assetTransfer;
    }

}
