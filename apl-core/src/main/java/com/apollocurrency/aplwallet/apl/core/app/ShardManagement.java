package com.apollocurrency.aplwallet.apl.core.app;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

/**
 * Interface for secondary shard management.
 */
public interface ShardManagement {

    /**
     * Find and return all available shards in main db
     * @param transactionalDataSource main db data source to search in
     * @return shard and file name
     */
    List<String> findAllShards(TransactionalDataSource transactionalDataSource);

    /**
     * Create new shard db or return existing shard by name
     * @param shardName shard name
     * @return opened data source
     */
    TransactionalDataSource createAndAddShard(String shardName);

}
