/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

/**
 * Interface for database shard management.
 *
 * @author yuriy.larin
 */
public interface ShardManagement {

    long TEMP_DB_IDENTITY = -1L;


    /**
     * Find and return all available shard Ids from main db 'SHARD' table
     *
     * @param transactionalDataSource main db data source to search for another shards in
     * @return shard and file name
     */
    List<Long> findAllShards(TransactionalDataSource transactionalDataSource);

    /**
     * That is preferred way to retrieve cached shard data source or create it fully initialized
     *
     * @param shardId shard Id to be added, can be NULL then an next shardId is selected from 'SHARD' table
     * @return shard database connection pool instance is put into internal cache
     */
    TransactionalDataSource getOrCreateShardDataSourceById(Long shardId);

    /**
     * That is preferred way to retrieve cached shard data source or create it fully or partially initialized.
     * The initialization schema is specified by dbVersion implementation class.
     *
     * @param shardId shard Id to be added, can be NULL then an next shardId is selected from 'SHARD' table
     * @param dbVersion 'partial' or 'full' kind of 'schema script' implementation class can be supplied
     * @return shard database connection pool instance is put into internal cache
     */
    TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DbVersion dbVersion);

    /**
     * Method gives ability to create 'temporary database' file with fully initialized internal schema.
     * The datasource is cached by -1L long value.
     *
     * @param temporaryDatabaseName temp database name
     * @return temp database data source
     */
    TransactionalDataSource createAndAddTemporaryDb(String temporaryDatabaseName);

    /**
     * Method gives ability to create new 'shard database' file with fully initialized internal schema.
     * It opens existing shard file and adds it into cached shard data source list.
     *
     * @param shardId shard Id to be added, can be NULL then an next shardId is selected from 'SHARD' table
     * @return shard database connection pool instance is put into internal cache
     */
    TransactionalDataSource createAndAddShard(Long shardId);

    /**
     * Method gives ability to create new 'shard database' file with partially initialized internal schema.
     * It opens existing shard file and adds it into cached shard data source list.
     * Partial schema is specified by dbVersion implementation
     *
     * @param shardId shard Id to be added, can be NULL then an next shardId is selected from 'SHARD' table
     * @param dbVersion 'partial' or 'full' kind of 'schema script' implementation class can be supplied
     * @return shard database connection pool instance is put into internal cache
     */
    TransactionalDataSource createAndAddShard(Long shardId, DbVersion dbVersion);

}
