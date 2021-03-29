package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEntity;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractRowMapper extends VersionedDerivedEntityMapper<SmcContractEntity> {

    public SmcContractRowMapper(KeyFactory<SmcContractEntity> keyFactory) {
        super(keyFactory);
    }

    @Override
    public SmcContractEntity doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long address = rs.getLong("address");
        long owner = rs.getLong("owner");
        long transactionId = rs.getLong("transaction_id");
        String data = rs.getString("data");
        String contractName = rs.getString("name");
        String languageName = rs.getString("language");
        String languageVersion = rs.getString("version");
        String args = rs.getString("args");
        String status = rs.getString("status");
        return new SmcContractEntity(null, null, address, owner, transactionId, data, contractName, args, languageName, languageVersion, status);
    }
}
