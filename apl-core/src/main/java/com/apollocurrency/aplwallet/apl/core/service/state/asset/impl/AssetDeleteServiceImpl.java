/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.asset.impl;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetDeleteTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDeleteService;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class AssetDeleteServiceImpl implements AssetDeleteService {

    private final AssetDeleteTable assetDeleteTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<AssetDelete> assetDeleteIteratorToStreamConverter;

    @Inject
    public AssetDeleteServiceImpl(AssetDeleteTable assetDeleteTable,
                                  BlockChainInfoService blockChainInfoService
    ) {
        this.assetDeleteTable = assetDeleteTable;
        this.blockChainInfoService = blockChainInfoService;
        this.assetDeleteIteratorToStreamConverter = new IteratorToStreamConverter<>();
    }

    /**
     * Constructor for unit tests
     */
    public AssetDeleteServiceImpl(AssetDeleteTable assetDeleteTable,
                                  BlockChainInfoService blockChainInfoService,
                                  IteratorToStreamConverter<AssetDelete> assetDeleteIteratorToStreamConverter // for unit tests mostly
    ) {
        this.assetDeleteTable = assetDeleteTable;
        this.blockChainInfoService = blockChainInfoService;
        if (assetDeleteIteratorToStreamConverter != null) { // for unit test only
            this.assetDeleteIteratorToStreamConverter = assetDeleteIteratorToStreamConverter;
        } else {
            this.assetDeleteIteratorToStreamConverter = new IteratorToStreamConverter<>();
        }
    }

    @Override
    public DbIterator<AssetDelete> getAssetDeletes(long assetId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    @Override
    public Stream<AssetDelete> getAssetDeletesStream(long assetId, int from, int to) {
        return assetDeleteIteratorToStreamConverter.apply(
            assetDeleteTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to));
    }

    @Override
    public DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("account_id", accountId),
            from, to, " ORDER BY height DESC, db_id DESC ");
    }

    @Override
    public Stream<AssetDelete> getAccountAssetDeletesStream(long accountId, int from, int to) {
        return assetDeleteIteratorToStreamConverter.apply(
            assetDeleteTable.getManyBy(new DbClause.LongClause("account_id", accountId),
                from, to, " ORDER BY height DESC, db_id DESC "));
    }

    @Override
    public DbIterator<AssetDelete> getAccountAssetDeletes(long accountId, long assetId, int from, int to) {
        return assetDeleteTable.getManyBy(new DbClause.LongClause("account_id", accountId).and(new DbClause.LongClause("asset_id", assetId)),
            from, to, " ORDER BY height DESC, db_id DESC ");
    }

    @Override
    public Stream<AssetDelete> getAccountAssetDeletesStream(long accountId, long assetId, int from, int to) {
        return assetDeleteIteratorToStreamConverter.apply(assetDeleteTable.getManyBy(
            new DbClause.LongClause("account_id", accountId)
                .and(new DbClause.LongClause("asset_id", assetId)),
            from, to, " ORDER BY height DESC, db_id DESC "));
    }

    @Override
    public AssetDelete addAssetDelete(Transaction transaction, long assetId, long quantityATU) {
        AssetDelete assetDelete = new AssetDelete(transaction, assetId, quantityATU,
            blockChainInfoService.getLastBlock().getTimestamp(), blockChainInfoService.getHeight());
        assetDelete.setHeight(blockChainInfoService.getHeight());
        assetDeleteTable.insert(assetDelete);
        return assetDelete;
    }

}
