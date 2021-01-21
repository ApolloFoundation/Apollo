/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface ShardDbExplorer {

    TransactionalDataSource getDataSourceWithSharding(long blockId);

    TransactionalDataSource getDataSourceWithShardingByHeight(int blockHeight);

    TransactionalDataSource getDatasourceWithShardingByTransactionId(long transactionId);

    TransactionalDataSource getShardDataSourceOrDefault(Long shardId);
}
