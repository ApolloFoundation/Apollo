package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import org.slf4j.Logger;

public class DbPreparedStatementWrapper extends FilteredPreparedStatement {
    private static final Logger log = getLogger(DbPreparedStatementWrapper.class);
    
    @Inject // it does not work, need to investigate why
    private static Blockchain blockchain;

    private long stmtThreshold;
    //TODO: Make injection work and delete this
    private void checkCDI(){
        if(blockchain==null){
            blockchain  = CDI.current().select(BlockchainImpl.class).get();
        }
    }
    public DbPreparedStatementWrapper(PreparedStatement stmt, String sql, long stmtThreshold) {
        super(stmt, sql);
        this.stmtThreshold = stmtThreshold;
    }

    @Override
    public boolean execute() throws SQLException {
        long start = System.currentTimeMillis();
        boolean b = super.execute();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold && blockchain != null) {
            checkCDI();
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, blockchain.getHeight(), getSQL()));
        }
        return b;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        long start = System.currentTimeMillis();
        ResultSet r = super.executeQuery();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold && blockchain != null) {
            checkCDI();
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, blockchain.getHeight(), getSQL()));
        }
        return r;
    }

    @Override
    public int executeUpdate() throws SQLException {
        long start = System.currentTimeMillis();
        int c = super.executeUpdate();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > stmtThreshold && blockchain != null) {
            checkCDI();
            logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                    (double)elapsed/1000.0, blockchain.getHeight(), getSQL()));
        }
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
