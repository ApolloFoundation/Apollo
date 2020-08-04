/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardRecovery;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * JDBI row mapper for {@link ShardRecovery}
 */
public class ShardRecoveryRowMapper implements RowMapper<ShardRecovery> {

    @Override
    public ShardRecovery map(ResultSet rs, StatementContext ctx) throws SQLException {

        return ShardRecovery.builder()
            .shardRecoveryId(rs.getLong("shard_recovery_id"))
            .state(rs.getString("state"))
            .objectName(rs.getString("object_name"))
            .columnName(rs.getString("column_name"))
            .lastColumnValue(rs.getLong("last_column_value"))
            .processedObject(rs.getString("processed_object"))
            .updated(Instant.ofEpochMilli(rs.getDate("updated").getTime()))
            .height(rs.getInt("height"))
            .build();
    }
}
