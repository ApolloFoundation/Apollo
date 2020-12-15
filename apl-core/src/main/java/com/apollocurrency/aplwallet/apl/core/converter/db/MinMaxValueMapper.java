/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.MinMaxValue;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class MinMaxValueMapper implements RowMapper<MinMaxValue> {

    @Override
    public MinMaxValue map(ResultSet rs, StatementContext ctx) throws SQLException {

        return new MinMaxValue(
            rs.getBigDecimal("MIN_ID"), // pagination is exclusive for lower bound
            rs.getBigDecimal("MAX_ID"), // pagination is exclusive for upper bound
            null,
            rs.getLong("COUNT"),
            rs.getInt("max_height")
        );
    }
}
