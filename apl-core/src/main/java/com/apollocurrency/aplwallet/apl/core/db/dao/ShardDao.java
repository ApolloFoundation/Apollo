package com.apollocurrency.aplwallet.apl.core.db.dao;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ShardRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Shard retrieving interface
 */
public interface ShardDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT " +
            "   shard_id as shard_id, " +
            "   shard_hash as shard_hash " +
            "FROM shard " +
            "WHERE shard_id = :shardId " +
            "ORDER BY id " +
            "LIMIT :limit")
    @RegisterBeanMapper(Shard.class)
    List<Shard> getShardBy(@Bind("shardId") long accountId, @Bind("limit") long limit);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard where shard_id = :shardId")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getShardById(@Bind("shardId") long accountId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard")
    @RegisterRowMapper(ShardRowMapper.class)
    List<Shard> getAllShard();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM shard")
    long countShard();

    @Transactional
    @SqlUpdate("INSERT INTO shard(shard_hash) " +
            "VALUES (:shardHash)")
    void saveShard(@BindBean Shard shard);

    @Transactional
    @SqlUpdate("UPDATE shard SET shard_hash = :shard_hash where shard_id = :shardId")
    void updateShard(@BindBean Shard shard);

    @Transactional
    @SqlUpdate("DELETE FROM shard where shard_id = :shardId")
    void hardDeleteShard(@Bind("id") long shardId);

    @Transactional
    @SqlUpdate("DELETE FROM shard")
    void hardDeleteAllShards();

}
