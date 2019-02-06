package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.PreparedStatement;
import java.sql.Statement;

public class DbFactory implements FilteredFactory {

    private long stmtThreshold;

    public DbFactory(long stmtThreshold) {
        this.stmtThreshold = stmtThreshold;
    }

    @Override
    public Statement createStatement(Statement stmt) {
        return new DbStatement(stmt, stmtThreshold);
    }

    @Override
    public PreparedStatement createPreparedStatement(PreparedStatement stmt, String sql) {
        return new DbPreparedStatement(stmt, sql, stmtThreshold);
    }
}
