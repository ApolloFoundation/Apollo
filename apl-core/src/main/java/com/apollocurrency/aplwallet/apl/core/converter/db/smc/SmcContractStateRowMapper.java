package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractStateEntity;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractStateRowMapper extends VersionedDerivedEntityMapper<SmcContractStateEntity> {

    public SmcContractStateRowMapper(KeyFactory<SmcContractStateEntity> keyFactory) {
        super(keyFactory);
    }

    @Override
    public SmcContractStateEntity doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long address = rs.getLong("address");
        String serializedObject = rs.getString("object");
        String status = rs.getString("status");

        return new SmcContractStateEntity(null, null, address, serializedObject, status);
    }
}
