package com.apollocurrency.aplwallet.apl.util.db;

import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.slf4j.LoggerFactory.getLogger;

public class DbStatementWrapper extends FilteredStatement {
    private static final Logger log = getLogger(DbStatementWrapper.class);

    private final long stmtThreshold;

    public DbStatementWrapper(Statement stmt, long stmtThreshold) {
        super(stmt);
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
    public boolean execute(String sql) throws SQLException {
        long start = System.currentTimeMillis();
        boolean b = super.execute(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds:\n%s",
                (double) elapsed / 1000.0, sql));
        return b;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        long start = System.currentTimeMillis();
        ResultSet r = super.executeQuery(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds:\n%s",
                (double) elapsed / 1000.0, sql));
        return r;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        long start = System.currentTimeMillis();
        int c = super.executeUpdate(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds:\n%s",
                (double) elapsed / 1000.0, sql));
        return c;
    }
}
