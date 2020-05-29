/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.service.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetTable;
import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AssetServiceImpl implements AssetService {

    private AssetTable assetTable;
    private BlockChainInfoService blockChainInfoService;
    private AssetDeleteService assetDeleteService;

    @Inject
    public AssetServiceImpl(AssetTable assetTable,
                            BlockChainInfoService blockChainInfoService,
                            AssetDeleteService assetDeleteService) {
        this.assetTable = assetTable;
        this.blockChainInfoService = blockChainInfoService;
        this.assetDeleteService = assetDeleteService;
    }

    @Override
    public DbIterator<Asset> getAllAssets(int from, int to) {
        return assetTable.getAll(from, to);
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
    public DbIterator<Asset> searchAssets(String query, int from, int to) {
        return assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC ");
    }

    @Override
    public void addAsset(Transaction transaction, ColoredCoinsAssetIssuance attachment) {
        assetTable.insert(new Asset(transaction, attachment, blockChainInfoService.getHeight()));
    }

    @Override
    public void deleteAsset(Transaction transaction, long assetId, long quantityATU) {
        Asset asset = getAsset(assetId);
        asset.setQuantityATU( Math.max(0, asset.getQuantityATU() - quantityATU) );
        asset.setHeight(blockChainInfoService.getHeight());
        assetTable.insert(asset);
        assetDeleteService.addAssetDelete(transaction, assetId, quantityATU);
    }

}
