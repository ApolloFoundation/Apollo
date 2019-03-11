package com.apollocurrency.aplwallet.apl.core.shard;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

/**
 * Interface for secondary shard management.
 */
public interface ShardManagement {

    /**
     * Find and return all available shard Ids from main db
     * @param transactionalDataSource main db data source to search for another shards in
     * @return shard and file name
     */
    List<Long> findAllShards(TransactionalDataSource transactionalDataSource);

    /**
     * Method gives ability to create new 'shard database', open existing shard and add it into shard list.
     * @param shardId shard name to be added
     * @return shard database connection pool instance
     */
    TransactionalDataSource createAndAddShard(Long shardId);

    TransactionalDataSource createAndAddShard(Long shardId, DbVersion dbVersion);

    TransactionalDataSource getOrCreateShardDataSourceById(Long shardId);

    TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DbVersion dbVersion);

    TransactionalDataSource createAndAddTemporaryDb(String temporaryDatabaseName);

}
