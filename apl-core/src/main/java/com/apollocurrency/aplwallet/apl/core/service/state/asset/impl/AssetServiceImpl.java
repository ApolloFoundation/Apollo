/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDeleteService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class AssetServiceImpl implements AssetService {

    private final AssetTable assetTable;
    private final BlockChainInfoService blockChainInfoService;
    private final AssetDeleteService assetDeleteService;
    private IteratorToStreamConverter<Asset> assetIteratorToStreamConverter;

    @Inject
    public AssetServiceImpl(AssetTable assetTable,
                            BlockChainInfoService blockChainInfoService,
                            AssetDeleteService assetDeleteService
    ) {
        this.assetTable = assetTable;
        this.blockChainInfoService = blockChainInfoService;
        this.assetDeleteService = assetDeleteService;
        this.assetIteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * Constructor for unit tests
     */
    public AssetServiceImpl(AssetTable assetTable,
                            BlockChainInfoService blockChainInfoService,
                            AssetDeleteService assetDeleteService,
                            IteratorToStreamConverter<Asset> assetIteratorToStreamConverter // for unit tests mostly
    ) {
        this.assetTable = assetTable;
        this.blockChainInfoService = blockChainInfoService;
        this.assetDeleteService = assetDeleteService;
        if (assetIteratorToStreamConverter != null) { // for unit tests
            this.assetIteratorToStreamConverter = assetIteratorToStreamConverter;
        } else {
            this.assetIteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
    }

    @Override
    public DbIterator<Asset> getAllAssets(int from, int to) {
        return assetTable.getAll(from, to);
    }

    @Override
    public Stream<Asset> getAllAssetsStream(int from, int to) {
        return assetIteratorToStreamConverter.apply(assetTable.getAll(from, to));
    }

    @Override
    public int getCount() {
        return assetTable.getCount();
    }

    @Override
    public Asset getAsset(long id) {
        return assetTable.get(AssetTable.assetDbKeyFactory.newKey(id));
    }

    @Override
    public Asset getAsset(long id, int height) {
        final DbKey dbKey = AssetTable.assetDbKeyFactory.newKey(id);
        if (height < 0 || blockChainInfoService.doesNotExceed(height)) {
            return assetTable.get(dbKey);
        }
        blockChainInfoService.checkAvailable(height);
        return assetTable.get(dbKey, height);
    }

    @Override
    public DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
        return assetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    @Override
    public Stream<Asset> getAssetsIssuedByStream(long accountId, int from, int to) {
        return assetIteratorToStreamConverter.apply(
            assetTable.getManyBy(
                new DbClause.LongClause("account_id", accountId), from, to));
    }

    @Override
    public DbIterator<Asset> searchAssets(String query, int from, int to) {
        return assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC ");
    }

    @Override
    public Stream<Asset> searchAssetsStream(String query, int from, int to) {
        return assetIteratorToStreamConverter.apply(
            assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC "));
    }

    @Override
    public void addAsset(Transaction transaction, ColoredCoinsAssetIssuance attachment) {
        assetTable.insert(new Asset(transaction, attachment, blockChainInfoService.getHeight()));
    }

    @Override
    public void deleteAsset(Transaction transaction, long assetId, long quantityATU) {
        Asset asset = getAsset(assetId);
        asset.setQuantityATU(Math.max(0, asset.getQuantityATU() - quantityATU));
        asset.setHeight(blockChainInfoService.getHeight());
        assetTable.insert(asset);
        assetDeleteService.addAssetDelete(transaction, assetId, quantityATU);
    }

}
