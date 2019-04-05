/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.Connection;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;

/**
 * Shard Recovery information management + retrieving interface.
 * It uses explicit connection to synchronize processed sharding data with recovery info within the same db transaction.
 */
public interface ShardRecoveryDaoJdbc {

    ShardRecovery getShardRecoveryById(Connection con, long shardRecoveryId);

    ShardRecovery getLatestShardRecovery(TransactionalDataSource sourceDataSource);

    ShardRecovery getLatestShardRecovery(Connection con);

    List<ShardRecovery> getAllShardRecovery(Connection con);

    long countShardRecovery(Connection con);

    long saveShardRecovery(TransactionalDataSource sourceDataSource, ShardRecovery shard);

    long saveShardRecovery(Connection con, ShardRecovery shard);

    int updateShardRecovery(TransactionalDataSource sourceDataSource, ShardRecovery shardRecovery);

    int updateShardRecovery(Connection con, ShardRecovery shardRecovery);

    int hardDeleteShardRecovery(Connection con, long shardRecoveryId);

    int hardDeleteAllShardRecovery(Connection con);

}