package com.apollocurrency.aplwallet.apl.dex.core.mapper;

import com.apollocurrency.aplwallet.apl.dex.core.model.DexOperation;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DexOperationMapper implements RowMapper<DexOperation> {

    @Override
    public DexOperation map(ResultSet rs, StatementContext ctx) throws SQLException {
        return DexOperation.builder()
            .dbId(rs.getLong("db_id"))
            .account(rs.getString("account"))
            .stage(DexOperation.Stage.from(rs.getByte("stage")))
            .description(rs.getString("description"))
            .eid(rs.getString("eid"))
            .details(rs.getString("details"))
            .finished(rs.getBoolean("finished"))
            .ts(rs.getTimestamp("ts"))
            .build();
    }
}
