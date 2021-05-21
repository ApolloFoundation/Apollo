package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractMappingEntity;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractMappingRowMapper extends VersionedDerivedEntityMapper<SmcContractMappingEntity> {

    public SmcContractMappingRowMapper(KeyFactory<SmcContractMappingEntity> keyFactory) {
        super(keyFactory);
    }

    @Override
    public SmcContractMappingEntity doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long address = rs.getLong("address");
        byte[] key = rs.getBytes("key");
        String name = rs.getString("name");
        String serializedObject = rs.getString("object");

        return new SmcContractMappingEntity(null, null, address, key, name, serializedObject);
    }
}
