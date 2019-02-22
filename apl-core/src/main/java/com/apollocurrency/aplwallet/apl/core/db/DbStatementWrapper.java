package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import org.slf4j.Logger;

public class DbStatementWrapper extends FilteredStatement {
    private static final Logger log = getLogger(DbStatementWrapper.class);
    private static Blockchain blockchain;

    private long stmtThreshold;

    public DbStatementWrapper(Statement stmt, long stmtThreshold) {
        super(stmt);
        this.stmtThreshold = stmtThreshold;
    }

    private Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(BlockchainImpl.class).get();
        }
        return blockchain;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        long start = System.currentTimeMillis();
        boolean b = super.execute(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, lookupBlockchain().getHeight(), sql));
        return b;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        long start = System.currentTimeMillis();
        ResultSet r = super.executeQuery(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, lookupBlockchain().getHeight(), sql));
        return r;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        long start = System.currentTimeMillis();
        int c = super.executeUpdate(sql);
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold)
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, lookupBlockchain().getHeight(), sql));
        return c;
    }

    private static void logThreshold(String msg) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(msg).append('\n');
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean firstLine = true;
        for (int i=3; i<stackTrace.length; i++) {
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
}
