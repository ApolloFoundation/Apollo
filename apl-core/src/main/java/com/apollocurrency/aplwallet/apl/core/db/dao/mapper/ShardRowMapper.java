package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * JDBI row mapper for {@link Shard}
 */
public class ShardRowMapper implements RowMapper<Shard> {

    @Override
    public Shard map(ResultSet rs, StatementContext ctx) throws SQLException {

        return Shard.builder()
                .id(rs.getLong("shard_id"))
                .shardHash(rs.getBytes("shard_hash"))
                .shardState(rs.getLong("shard_state"))
                .build();
    }
}
