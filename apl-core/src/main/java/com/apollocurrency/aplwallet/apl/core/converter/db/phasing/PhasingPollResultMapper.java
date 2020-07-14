/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db.phasing;

import com.apollocurrency.aplwallet.apl.core.converter.db.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollResultMapper extends DerivedEntityMapper<PhasingPollResult> {

    public PhasingPollResultMapper(KeyFactory<PhasingPollResult> keyFactory) {
        super(keyFactory);
    }

    @Override
    public PhasingPollResult doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long result = rs.getLong("result");
        boolean approved = rs.getBoolean("approved");
        return new PhasingPollResult(null, null, id, result, approved);
    }
}
