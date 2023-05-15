/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

public class DbPreparedStatementWrapper extends FilteredPreparedStatement {
    private static final Logger log = getLogger(DbPreparedStatementWrapper.class);

    private long stmtThreshold;

    public DbPreparedStatementWrapper(PreparedStatement stmt, String sql, long stmtThreshold) {
        super(stmt, sql);
        this.stmtThreshold = stmtThreshold;
    }

    private static void logThreshold(String msg) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(msg).append('\n');
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean firstLine = true;
        for (int i = 3; i < stackTrace.length; i++) {
            String line = stackTrace[i].toString();
            if (!line.startsWith("apl."))
                break;
            if (firstLine)
                firstLine = false;
            else
                sb.append('\n');
            sb.append("  ").append(line);
        }
        log.debug(sb.toString());
    }

    @Override
    public boolean execute() throws SQLException {
        long start = System.currentTimeMillis();
        boolean b = super.execute();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold) {
            logThreshold(String.format("SQL statement required %.3f seconds:\n%s",
                (double) elapsed / 1000.0, getSQL()));
        }
        return b;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        long start = System.currentTimeMillis();
        ResultSet r = super.executeQuery();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold) {
            logThreshold(String.format("SQL statement required %.3f seconds:\n%s",
                (double) elapsed / 1000.0, getSQL()));
        }
        return r;
    }

    @Override
    public int executeUpdate() throws SQLException {
        long start = System.currentTimeMillis();
        int c = super.executeUpdate();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold) {
            logThreshold(String.format("SQL statement required %.3f seconds:\n%s",
                (double) elapsed / 1000.0, getSQL()));
        }
        return c;
    }
}
