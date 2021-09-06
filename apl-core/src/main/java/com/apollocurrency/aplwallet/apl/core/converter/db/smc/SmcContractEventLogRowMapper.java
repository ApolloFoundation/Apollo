package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventLogEntry;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractEventLogRowMapper implements RowMapper<SmcContractEventLogEntry> {

    @Override
    public SmcContractEventLogEntry map(ResultSet rs, StatementContext ctx) throws SQLException {
        long dbId = rs.getLong("db_id");
        long eventId = rs.getLong("event_id");
        long transactionId = rs.getLong("transaction_id");
        byte[] signature = rs.getBytes("signature");
        String entry = rs.getString("state");
        int txIdx = rs.getByte("tx_idx");

        int height = rs.getInt("height");

        return new SmcContractEventLogEntry(dbId, height, dbId, eventId, transactionId, signature, entry, txIdx);
    }
}
