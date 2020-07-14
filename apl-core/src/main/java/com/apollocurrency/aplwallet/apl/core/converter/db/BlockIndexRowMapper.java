package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.BlockIndex;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBI row mapper for {@link BlockIndex}
 */
public class BlockIndexRowMapper implements RowMapper<BlockIndex> {

    @Override
    public BlockIndex map(ResultSet rs, StatementContext ctx) throws SQLException {

        return BlockIndex.builder()
            .blockId(rs.getLong("block_id"))
            .blockHeight(rs.getInt("block_height"))
            .build();
    }
}
