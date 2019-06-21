package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBI row mapper for {@link Shard}
 */
public class ShardRowMapper implements RowMapper<Shard> {

    @Override
    public Shard map(ResultSet rs, StatementContext ctx) throws SQLException {

        Long[] generatorIds = DbUtils.getArray(rs, "generator_ids", Long[].class);
        generatorIds = generatorIds == null ? Convert.EMPTY_OBJECT_LONG : generatorIds;
        return Shard.builder()
                .id(rs.getLong("shard_id"))
                .shardHash(rs.getBytes("shard_hash"))
                .shardState(rs.getLong("shard_state"))
                .shardHeight(rs.getInt("shard_height"))
                .zipHashCrc(rs.getBytes("zip_hash_crc"))
                .generatorIds(Convert.toArray(generatorIds)) // should not be empty
                .build();
    }
}
