/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ShardRecoveryRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Shard Recovery information management + retrieving interface
 */
public interface ShardRecoveryDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard_recovery where shard_recovery_id = :shardRecoveryId")
    @RegisterRowMapper(ShardRecoveryRowMapper.class)
    ShardRecovery getShardRecoveryById(@Bind("shardRecoveryId") long shardRecoveryId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard_recovery limit 1")
    @RegisterRowMapper(ShardRecoveryRowMapper.class)
    ShardRecovery getLatestShardRecovery();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard_recovery")
    @RegisterRowMapper(ShardRecoveryRowMapper.class)
    List<ShardRecovery> getAllShardRecovery();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM shard_recovery")
    long countShardRecovery();

    @Transactional
    @SqlUpdate("INSERT INTO shard_recovery(" +
        "shard_recovery_id, state, object_name, column_name, last_column_value, processed_object, updated, height) " +
        "VALUES (:shardRecoveryId, :state, :objectName, :columnName, :lastColumnValue, :processedObject, CURRENT_TIMESTAMP(), :height)")
    @RegisterRowMapper(ShardRecoveryRowMapper.class)
    @GetGeneratedKeys
    long saveShardRecovery(@BindBean ShardRecovery shard);

    @Transactional
    @SqlUpdate("UPDATE shard_recovery SET state=:state, object_name=:objectName, column_name=:columnName, " +
        "last_column_value=:lastColumnValue, processed_object=:processedObject, updated=CURRENT_TIMESTAMP() " +
        "where shard_recovery_id =:shardRecoveryId")
    @RegisterRowMapper(ShardRecoveryRowMapper.class)
    int updateShardRecovery(@BindBean ShardRecovery shardRecovery);

    @Transactional
    @SqlUpdate("DELETE FROM shard_recovery where shard_recovery_id=:shardRecoveryId")
    int hardDeleteShardRecovery(@Bind("shardRecoveryId") long shardRecoveryId);

    @Transactional
    @SqlUpdate("DELETE FROM shard_recovery")
    int hardDeleteAllShardRecovery();

}
