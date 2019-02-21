package com.apollocurrency.aplwallet.apl.core.app;

import java.util.List;

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
     * Create new shard db or return existing shard by name
     * @param shardId shard Id, it will be formatted into String shard file name later
     * @return opened data source
     */
    TransactionalDataSource createAndAddShard(Long shardId);

    TransactionalDataSource getShardDataSourceById(Long shardId);

}
