package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.BlockIndexRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Global Block Index management + retrieving interface
 */
public interface BlockIndexDao {

    /**
     * For Unit tests ONLY
     * @param shardId shard id
     * @param limit limit number or rows
     * @return found records list
     */
    @Transactional(readOnly = true)
    @SqlQuery("SELECT " +
            "   shard_id, " +
            "   block_id " +
            "   block_height " +
            "FROM block_index " +
            "WHERE shard_id = :shardId " +
            "ORDER BY block_id " +
            "LIMIT :limit")
    @RegisterBeanMapper(BlockIndex.class)
    @RegisterRowMapper(BlockIndexRowMapper.class)
    List<BlockIndex> getByShardId(@Bind("shardId") long shardId, @Bind("limit") long limit);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index where block_id = :blockId")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    BlockIndex getByBlockId(@Bind("blockId") long blockId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT shard_id FROM block_index where block_id = :blockId")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    Long getShardIdByBlockId(@Bind("blockId") long blockId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index where block_height = :blockHeight")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    BlockIndex getByBlockHeight(@Bind("blockHeight") int blockHeight);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT shard_id FROM block_index where block_height = :blockHeight")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    Long getShardIdByBlockHeight(@Bind("blockHeight") int blockHeight);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    List<BlockIndex> getAllBlockIndex();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index ORDER BY block_height desc LIMIT 1")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    BlockIndex getLast();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT block_height FROM block_index ORDER BY block_height desc LIMIT 1")
    Integer getLastHeight();

    @Transactional(readOnly = true)
    @SqlQuery("select count(*) from block_index")
    int count();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM block_index where shard_id =:shardId")
    long countBlockIndexByShard(@Bind("shardId") long shardId);

    @Transactional
    @SqlUpdate("INSERT INTO block_index(shard_id, block_id, block_height) " +
            "VALUES (:shardId, :blockId, :blockHeight)")
    int saveBlockIndex(@BindBean BlockIndex blockIndex);

    @Transactional
    @SqlUpdate("UPDATE block_index SET block_id = :blockId, block_height =:blockHeight where shard_id = :shardId AND block_id =:blockId")
    int updateBlockIndex(@BindBean BlockIndex blockIndex);

    @Transactional
    @SqlUpdate("DELETE FROM block_index where shard_id = :shardId AND block_id =:blockId")
    int hardBlockIndex(@BindBean BlockIndex blockIndex);

    @Transactional
    @SqlUpdate("DELETE FROM block_index")
    int hardDeleteAllBlockIndex();

}
