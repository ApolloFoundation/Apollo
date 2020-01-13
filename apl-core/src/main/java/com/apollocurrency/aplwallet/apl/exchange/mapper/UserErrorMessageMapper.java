package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class UserErrorMessageMapper implements RowMapper<UserErrorMessage> {
    @Override
    public UserErrorMessage map(ResultSet rs, StatementContext ctx) throws SQLException {
        long dbId = rs.getLong("db_id");
        String address = rs.getString("address");
        String error = rs.getString("error");
        String operation = rs.getString("operation");
        String details = rs.getString("details");
        long timestamp = rs.getLong("timestamp");
        return new UserErrorMessage(dbId, address, error,operation, details, timestamp);
    }
}
