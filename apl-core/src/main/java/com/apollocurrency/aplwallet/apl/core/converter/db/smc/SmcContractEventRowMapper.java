package com.apollocurrency.aplwallet.apl.core.converter.db.smc;

import com.apollocurrency.aplwallet.apl.core.converter.db.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.smc.SmcContractEventEntity;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SmcContractEventRowMapper extends DerivedEntityMapper<SmcContractEventEntity> {

    public SmcContractEventRowMapper(KeyFactory<SmcContractEventEntity> keyFactory) {
        super(keyFactory);
    }

    @Override
    public SmcContractEventEntity doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long address = rs.getLong("address");
        long transactionId = rs.getLong("transaction_id");
        byte[] signature = rs.getBytes("signature");
        String name = rs.getString("name");
        byte idxCount = rs.getByte("idx_count");
        boolean anonymous = rs.getBoolean("is_anonymous");

        return new SmcContractEventEntity(null, null, id, address, transactionId, signature, name, idxCount, anonymous);
    }
}
