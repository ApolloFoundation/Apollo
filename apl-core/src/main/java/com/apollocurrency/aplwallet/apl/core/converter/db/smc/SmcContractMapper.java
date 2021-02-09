package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContract;
import org.jdbi.v3.core.statement.StatementContext;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractMapper extends VersionedDerivedEntityMapper<SmcContract> {

    public SmcContractMapper(KeyFactory<SmcContract> keyFactory) {
        super(keyFactory);
    }

    @Override
    public SmcContract doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        String address = rs.getString("address");
        String data = rs.getString("data");
        String contractName = rs.getString("name");
        String languageName = rs.getString("language");
        BigInteger fuelValue = new BigInteger(rs.getBytes("fuel"));
        BigInteger fuelPrice = new BigInteger(rs.getBytes("fuel_price"));
        String transactionId = rs.getString("transaction_id");
        return new SmcContract(null, null, address, data, contractName, languageName, fuelValue, fuelPrice, transactionId);
    }
}
