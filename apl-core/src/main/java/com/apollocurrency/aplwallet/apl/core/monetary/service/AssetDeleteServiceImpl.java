/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.service.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.monetary.dao.AssetDeleteTable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AssetDeleteServiceImpl implements AssetDeleteService {

    private AssetDeleteTable assetDeleteTable;
    private BlockChainInfoService blockChainInfoService;

    @Inject
    public AssetDeleteServiceImpl(AssetDeleteTable assetDeleteTable, BlockChainInfoService blockChainInfoService) {
        this.assetDeleteTable = assetDeleteTable;
        this.blockChainInfoService = blockChainInfoService;
    }

    @Override
    public DbIterator<AssetDelete> getAssetDeletes(long assetId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    @Override
    public DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to, " ORDER BY height DESC, db_id DESC ");
    }

    @Override
    public DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, long assetId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("account_id", accountId).and(new DbClause.LongClause("asset_id", assetId)),
            from, to, " ORDER BY height DESC, db_id DESC ");
    }

    @Override
    public AssetDelete addAssetDelete(Transaction transaction, long assetId, long quantityATU) {
        AssetDelete assetDelete = new AssetDelete(transaction, assetId, quantityATU,
            blockChainInfoService.getLastBlock().getTimestamp(), blockChainInfoService.getHeight());
        assetDeleteTable.insert(assetDelete);
        return assetDelete;
    }

}
