/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.mapper;

import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.ShufflingData;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShufflingDataMapper extends DerivedEntityMapper<ShufflingData> {

    public ShufflingDataMapper(KeyFactory<ShufflingData> keyFactory) {
        super(keyFactory);
    }

    @Override
    public ShufflingData doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long shufflingId = rs.getLong("shuffling_id");
        long accountId = rs.getLong("account_id");
        byte[][] data = DbUtils.getArray(rs, "data", byte[][].class, Convert.EMPTY_BYTES);
        int transactionTimestamp = rs.getInt("transaction_timestamp");
        return new ShufflingData(null, null, shufflingId, accountId, data, transactionTimestamp);
    }
}
