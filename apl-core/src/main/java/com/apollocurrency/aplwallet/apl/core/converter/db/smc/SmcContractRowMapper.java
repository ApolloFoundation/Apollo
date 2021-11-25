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
        return SmcContractEntity.builder()
            .address(rs.getLong("address"))
            .owner(rs.getLong("owner"))
            .transactionId(rs.getLong("transaction_id"))
            .transactionTimestamp(rs.getInt("transaction_timestamp"))
            .transactionHash(rs.getBytes("transaction_hash"))
            .data(rs.getString("data"))
            .contractName(rs.getString("name"))
            .baseContract(rs.getString("base_contract"))
            .languageName(rs.getString("language"))
            .languageVersion(rs.getString("version"))
            .args(rs.getString("args"))
            .status(rs.getString("status"))
            .build();
    }
}
