/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.mapper;

import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhasingPollResultMapper implements RowMapper<PhasingPollResult> {
    @Override
    public PhasingPollResult map(ResultSet rs, StatementContext ctx) throws SQLException {
        long id = rs.getLong("id");
        long result = rs.getLong("result");
        boolean approved = rs.getBoolean("approved");
        int height = rs.getInt("height");
        return new PhasingPollResult(id, result, approved, height);
    }
}
