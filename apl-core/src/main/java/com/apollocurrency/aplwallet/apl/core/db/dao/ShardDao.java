package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ShardRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Shard management + retrieving interface
 */
public interface ShardDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard where shard_id = :shardId")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getShardById(@Bind("shardId") long shardId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard")
    @RegisterRowMapper(ShardRowMapper.class)
    List<Shard> getAllShard();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM shard")
    long countShard();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT IFNULL(max(SHARD_ID) + 1, 1) as shard_id FROM shard")
    long getNextShardId();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT IFNULL(max(SHARD_ID) + 1, 1) FROM shard")
    long getMaxShardId();

    @Transactional
    @SqlUpdate("INSERT INTO shard(shard_id, shard_hash) VALUES (:shardId, :shardHash)")
    @RegisterRowMapper(ShardRowMapper.class)
    int saveShard(@BindBean Shard shard);

    @Transactional
    @SqlUpdate("UPDATE shard SET shard_hash =:shardHash where shard_id =:shardId")
    @RegisterRowMapper(ShardRowMapper.class)
    int updateShard(@BindBean Shard shard);

    @Transactional
    @SqlUpdate("DELETE FROM shard where shard_id =:shardId")
    int hardDeleteShard(@Bind("shardId") long shardId);

    @Transactional
    @SqlUpdate("DELETE FROM shard")
    int hardDeleteAllShards();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_id = (SELECT shard_id FROM block_index WHERE block_height = :height)")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getShardAtHeight(@Bind("height") int height);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM shard WHERE shard_id = (SELECT shard_id FROM block_index WHERE block_height = (SELECT MAX(block_height) FROM block_index) )")
    @RegisterRowMapper(ShardRowMapper.class)
    Shard getLastShard();

}
