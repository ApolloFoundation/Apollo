package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractState;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractStateMapper extends VersionedDerivedEntityMapper<SmcContractState> {

    public SmcContractStateMapper(KeyFactory<SmcContractState> keyFactory) {
        super(keyFactory);
    }

    @Override
    public SmcContractState doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        String address = rs.getString("address");
        String transactionId = rs.getString("transaction_id");
        String method = rs.getString("method");
        String args = rs.getString("args");
        String serializedObject = rs.getString("object");
        String status = rs.getString("status");


        return new SmcContractState(null, null, address, transactionId, method, args, status, serializedObject);
    }
}
