package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class FilteredFactoryImpl implements FilteredFactory {

    private long stmtThreshold;

    public FilteredFactoryImpl(long stmtThreshold) {
        this.stmtThreshold = stmtThreshold;
    }

    @Override
    public Statement createStatement(Statement stmt) {
        return new DbStatementWrapper(stmt, stmtThreshold);
    }

    @Override
    public PreparedStatement createPreparedStatement(PreparedStatement stmt, String sql) {
        return new DbPreparedStatementWrapper(stmt, sql, stmtThreshold);
    }
}
