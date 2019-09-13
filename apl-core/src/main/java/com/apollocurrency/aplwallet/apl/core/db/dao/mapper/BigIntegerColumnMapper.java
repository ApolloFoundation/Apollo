package com.apollocurrency.aplwallet.apl.core.db.dao.mapper;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Convert from database long into java BigInteger
 */
public class BigIntegerColumnMapper implements ColumnMapper<BigInteger> {

    public BigIntegerColumnMapper() {
    }

    @Override
    public BigInteger map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        String string = r.getString(columnNumber);
        return Optional.ofNullable(string).map(BigInteger::new).orElse(null);
    }
}
