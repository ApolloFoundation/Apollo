/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class ShardDbExplorerImpl implements ShardDbExplorer {
    private final DatabaseManager databaseManager;
    private final BlockIndexService blockIndexService;
    private final TransactionIndexDao transactionIndexDao;

    @Inject
    public ShardDbExplorerImpl(DatabaseManager databaseManager, BlockIndexService blockIndexService, TransactionIndexDao transactionIndexDao) {
        this.databaseManager = databaseManager;
        this.blockIndexService = blockIndexService;
        this.transactionIndexDao = transactionIndexDao;
    }

    @Override
    public TransactionalDataSource getDataSourceWithSharding(long blockId) {
        Long shardId = blockIndexService.getShardIdByBlockId(blockId);
        return getShardDataSourceOrDefault(shardId);
    }

    @Override
    public TransactionalDataSource getDataSourceWithShardingByHeight(int blockHeight) {
        Long shardId = blockIndexService.getShardIdByBlockHeight(blockHeight);
        return getShardDataSourceOrDefault(shardId);
    }

    @Override
    public TransactionalDataSource getDatasourceWithShardingByTransactionId(long transactionId) {
        Long shardId = transactionIndexDao.getShardIdByTransactionId(transactionId);
        return getShardDataSourceOrDefault(shardId);
    }

    @Override
    public TransactionalDataSource getShardDataSourceOrDefault(Long shardId) {
        TransactionalDataSource dataSource = null;
        if (shardId != null) {
            dataSource = ((ShardManagement) databaseManager).getOrInitFullShardDataSourceById(shardId);
        }
        if (dataSource == null) {
            dataSource = databaseManager.getDataSource();
        }
        return dataSource;
    }

}
