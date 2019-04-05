package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * JDBI row mapper for {@link BlockIndex}
 */
public class BlockIndexRowMapper implements RowMapper<BlockIndex> {

    @Override
    public BlockIndex map(ResultSet rs, StatementContext ctx) throws SQLException {

        return BlockIndex.builder()
                .shardId(rs.getLong("shard_id"))
                .blockId(rs.getLong("block_id"))
                .blockHeight(rs.getInt("block_height"))
                .build();
    }
}
