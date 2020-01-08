package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.DexOperation;
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
            .stage(DexOperation.Stage.from(rs.getInt("stage")))
            .description(rs.getString("description"))
            .exchangeId(rs.getLong("exchange_id"))
            .time(rs.getTimestamp("time"))
            .details(rs.getString("details"))
            .build();
    }
}
