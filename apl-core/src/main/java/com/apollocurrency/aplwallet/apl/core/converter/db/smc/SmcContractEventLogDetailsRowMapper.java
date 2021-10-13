package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.api.v2.model.SmcContractEvent;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractEventLogDetailsRowMapper implements RowMapper<SmcContractEvent> {

    @Override
    public SmcContractEvent map(ResultSet rs, StatementContext ctx) throws SQLException {
        long dbId = rs.getLong("db_id");
        long eventId = rs.getLong("event_id");
        long transactionId = rs.getLong("transaction_id");
        byte[] signature = rs.getBytes("signature");
        String state = rs.getString("state");
        int txIdx = rs.getByte("tx_idx");

        int height = rs.getInt("height");

        long address = rs.getLong("contract");
        String spec = rs.getString("spec");
        String name = rs.getString("name");

        var contractEvent = new SmcContractEvent();
        contractEvent.setName(name);
        contractEvent.setSpec(spec);
        contractEvent.setContract(Convert.toHexString(address));
        contractEvent.setHeight(height);
        contractEvent.setTransactionIndex(txIdx);
        contractEvent.setData(state);
        contractEvent.setSignature(Convert.toHexString(signature));
        contractEvent.setTransaction(Convert.toHexString(transactionId));
        contractEvent.setEvent(Convert.toHexString(eventId));

        return contractEvent;
    }
}
