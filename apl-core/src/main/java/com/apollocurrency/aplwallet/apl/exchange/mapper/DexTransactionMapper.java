package com.apollocurrency.aplwallet.apl.exchange.mapper;

import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DexTransactionMapper implements RowMapper<DexTransaction> {
    @Override
    public DexTransaction map(ResultSet rs, StatementContext ctx) throws SQLException {
        long dbId = rs.getLong("db_id");
        byte[] hash = rs.getBytes("hash");
        byte[] rawTransactionBytes = rs.getBytes("tx");
        byte operation = rs.getByte("operation");
        String params = rs.getString("params");
        String address = rs.getString("address");
        long timestamp = rs.getLong("timestamp");
        return new DexTransaction(dbId, hash, rawTransactionBytes, DexTransaction.DexOperation.from(operation), params, address, timestamp);
    }
}
