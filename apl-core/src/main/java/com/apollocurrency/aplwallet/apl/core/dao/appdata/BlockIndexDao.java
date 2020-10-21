package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.converter.db.BlockIndexRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.BlockIndex;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
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

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index where block_id = :blockId")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    BlockIndex getByBlockId(@Bind("blockId") long blockId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT shard_id FROM shard WHERE shard_height > (SELECT block_height from block_index where block_id = :blockId) ORDER BY shard_height ASC LIMIT 1")
    Long getShardIdByBlockId(@Bind("blockId") long blockId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index where block_height = :blockHeight")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    BlockIndex getByBlockHeight(@Bind("blockHeight") int blockHeight);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT shard_id FROM shard where shard_height > :blockHeight ORDER BY shard_height LIMIT 1")
    Long getShardIdByBlockHeight(@Bind("blockHeight") int blockHeight);


    @Transactional(readOnly = true)
    @SqlQuery("SELECT block_height FROM block_index where block_id = :id")
    Integer getHeight(@Bind("id") long id);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index order by block_height")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    List<BlockIndex> getAllBlockIndex();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM block_index ORDER BY block_height desc LIMIT 1")
    @RegisterRowMapper(BlockIndexRowMapper.class)
    BlockIndex getLast();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT max(block_height) FROM block_index")
    Integer getLastHeight();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT block_id FROM block_index WHERE block_height > :height ORDER BY block_height asc LIMIT :limit")
    List<Long> getBlockIdsAfter(@Bind("height") int height, @Bind("limit") int limit);


    @Transactional(readOnly = true)
    @SqlQuery("select count(*) from block_index")
    int count();

    @Transactional(readOnly = true)
    @SqlQuery("SELECT count(*) FROM block_index where block_height < IFNULL((select shard_height from shard where shard_id =:shardId),0) AND block_height >= IFNULL((select shard_height from shard where shard_height < (select shard_height from shard where shard_id =:shardId) ORDER BY shard_height desc LIMIT 1),0)")
    @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
    long countBlockIndexByShard(@Bind("shardId") long shardId);

    @Transactional
    @SqlUpdate("INSERT INTO block_index(block_id, block_height) " +
        "VALUES (:blockId, :blockHeight)")
    int saveBlockIndex(@BindBean BlockIndex blockIndex);

    @Transactional
    @SqlUpdate("UPDATE block_index SET block_height =:blockHeight where block_id =:blockId")
    int updateBlockIndex(@BindBean BlockIndex blockIndex);

    @Transactional
    @SqlUpdate("DELETE FROM block_index where block_id =:blockId")
    int hardDeleteBlockIndex(@BindBean BlockIndex blockIndex);

    @Transactional
    @SqlUpdate("DELETE FROM block_index")
    int hardDeleteAllBlockIndex();

}
