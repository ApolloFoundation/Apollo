/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.util.Logger;
import net.sf.log4jdbc.ConnectionSpy;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransactionalDb extends BasicDb {

    private static final DbFactory factory = new DbFactory();
    private static final long stmtThreshold;
    private static final long txThreshold;
    private static final long txInterval;
    private static final boolean enableSqlLogs;
    static {
        stmtThreshold = getPropertyOrDefault("apl.statementLogThreshold", 1000);
        txThreshold = getPropertyOrDefault("apl.transactionLogThreshold", 5000);
        txInterval = getPropertyOrDefault("apl.transactionLogInterval", 15) * 60 * 1000;
        enableSqlLogs = Apl.getBooleanProperty("apl.enableSqlLogs");
    }

    private final ThreadLocal<DbConnection> localConnection = new ThreadLocal<>();
    private final ThreadLocal<Map<String,Map<DbKey,Object>>> transactionCaches = new ThreadLocal<>();
    private final ThreadLocal<Set<TransactionCallback>> transactionCallback = new ThreadLocal<>();
    private volatile long txTimes = 0;
    private volatile long txCount = 0;
    private volatile long statsTime = 0;

    public TransactionalDb(DbProperties dbProperties) {
        super(dbProperties);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection con = localConnection.get();
        if (con != null) {
            return enableSqlLogs ? new ConnectionSpy(con) : con;
        }
        DbConnection realConnection = new DbConnection(super.getConnection());
        return enableSqlLogs ? new ConnectionSpy(realConnection) : realConnection;
    }

    public Connection getConnection(boolean doSqlLog) throws SQLException {
        if (!enableSqlLogs && doSqlLog) {
            return new ConnectionSpy(getConnection());
        }
        return getConnection();
    }

    public boolean isInTransaction() {
        return localConnection.get() != null;
    }

    public Connection beginTransaction() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = getPooledConnection();
            con.setAutoCommit(false);
            con = new DbConnection(con);
            ((DbConnection)con).txStart = System.currentTimeMillis();
            localConnection.set((DbConnection)con);
            transactionCaches.set(new HashMap<>());
            return enableSqlLogs ? new ConnectionSpy(con) : con;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    public Connection beginTransaction(boolean doSqlLog) {
        if (!enableSqlLogs && doSqlLog) {
            return new ConnectionSpy(beginTransaction());
        }
        return beginTransaction();
    }

    public void commitTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doCommit();
            Set<TransactionCallback> callbacks = transactionCallback.get();
            if (callbacks != null) {
                callbacks.forEach(TransactionCallback::commit);
                transactionCallback.set(null);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void rollbackTransaction() {
        DbConnection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doRollback();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            transactionCaches.get().clear();
            Set<TransactionCallback> callbacks = transactionCallback.get();
            if (callbacks != null) {
                callbacks.forEach(TransactionCallback::rollback);
                transactionCallback.set(null);
            }
        }
    }

    public void endTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        localConnection.set(null);
        transactionCaches.set(null);
        long now = System.currentTimeMillis();
        long elapsed = now - ((DbConnection)con).txStart;
        if (elapsed >= txThreshold) {
            logThreshold(String.format("Database transaction required %.3f seconds at height %d",
                                       (double)elapsed/1000.0, Apl.getBlockchain().getHeight()));
        } else {
            long count, times;
            boolean logStats = false;
            synchronized(this) {
                count = ++txCount;
                times = txTimes += elapsed;
                if (now - statsTime >= txInterval) {
                    logStats = true;
                    txCount = 0;
                    txTimes = 0;
                    statsTime = now;
                }
            }
            if (logStats)
                Logger.logDebugMessage(String.format("Average database transaction time is %.3f seconds",
                                                     (double)times/1000.0/(double)count));
        }
        DbUtils.close(con);
    }

    public void registerCallback(TransactionCallback callback) {
        Set<TransactionCallback> callbacks = transactionCallback.get();
        if (callbacks == null) {
            callbacks = new HashSet<>();
            transactionCallback.set(callbacks);
        }
        callbacks.add(callback);
    }

    Map<DbKey,Object> getCache(String tableName) {
        if (!isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        Map<DbKey,Object> cacheMap = transactionCaches.get().get(tableName);
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
            transactionCaches.get().put(tableName, cacheMap);
        }
        return cacheMap;
    }

    void clearCache(String tableName) {
        Map<DbKey,Object> cacheMap = transactionCaches.get().get(tableName);
        if (cacheMap != null) {
            cacheMap.clear();
        }
    }

    public void clearCache() {
        transactionCaches.get().values().forEach(Map::clear);
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
        Logger.logDebugMessage(sb.toString());
    }

    private final class DbConnection extends FilteredConnection {

        long txStart = 0;

        private DbConnection(Connection con) {
            super(con, factory);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            throw new UnsupportedOperationException("Use Db.beginTransaction() to start a new transaction");
        }

        @Override
        public void commit() throws SQLException {
            if (localConnection.get() == null) {
                super.commit();
            } else if (this != localConnection.get()) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                commitTransaction();
            }
        }

        private void doCommit() throws SQLException {
            super.commit();
        }

        @Override
        public void rollback() throws SQLException {
            if (localConnection.get() == null) {
                super.rollback();
            } else if (this != localConnection.get()) {
                throw new IllegalStateException("Previous connection not committed");
            } else {
                rollbackTransaction();
            }
        }

        private void doRollback() throws SQLException {
            super.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (localConnection.get() == null) {
                super.close();
            } else if (this != localConnection.get()) {
                throw new IllegalStateException("Previous connection not committed");
            }
        }
    }

    private static final class DbStatement extends FilteredStatement {

        private DbStatement(Statement stmt) {
            super(stmt);
        }

        @Override
        public boolean execute(String sql) throws SQLException {
            long start = System.currentTimeMillis();
            boolean b = super.execute(sql);
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > stmtThreshold)
                logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                                           (double)elapsed/1000.0, Apl.getBlockchain().getHeight(), sql));
            return b;
        }

        @Override
        public ResultSet executeQuery(String sql) throws SQLException {
            long start = System.currentTimeMillis();
            ResultSet r = super.executeQuery(sql);
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > stmtThreshold)
                logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                                           (double)elapsed/1000.0, Apl.getBlockchain().getHeight(), sql));
            return r;
        }

        @Override
        public int executeUpdate(String sql) throws SQLException {
            long start = System.currentTimeMillis();
            int c = super.executeUpdate(sql);
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > stmtThreshold)
                logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                                           (double)elapsed/1000.0, Apl.getBlockchain().getHeight(), sql));
            return c;
        }
    }

    private static final class DbPreparedStatement extends FilteredPreparedStatement {
        private DbPreparedStatement(PreparedStatement stmt, String sql) {
            super(stmt, sql);
        }

        @Override
        public boolean execute() throws SQLException {
            long start = System.currentTimeMillis();
            boolean b = super.execute();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > stmtThreshold)
                logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                                           (double)elapsed/1000.0, Apl.getBlockchain().getHeight(), getSQL()));
            return b;
        }

        @Override
        public ResultSet executeQuery() throws SQLException {
            long start = System.currentTimeMillis();
            ResultSet r = super.executeQuery();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > stmtThreshold)
                logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                                           (double)elapsed/1000.0, Apl.getBlockchain().getHeight(), getSQL()));
            return r;
        }

        @Override
        public int executeUpdate() throws SQLException {
            long start = System.currentTimeMillis();
            int c = super.executeUpdate();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > stmtThreshold)
                logThreshold(String.format("SQL statement required %.3f seconds at height %d:\n%s",
                                           (double)elapsed/1000.0, Apl.getBlockchain().getHeight(), getSQL()));
            return c;
        }
    }

    private static final class DbFactory implements FilteredFactory {

        @Override
        public Statement createStatement(Statement stmt) {
            return new DbStatement(stmt);
        }

        @Override
        public PreparedStatement createPreparedStatement(PreparedStatement stmt, String sql) {
            return new DbPreparedStatement(stmt, sql);
        }
    }

    /**
     * Transaction callback interface
     */
    public interface TransactionCallback {

        /**
         * Transaction has been committed
         */
        void commit();

        /**
         * Transaction has been rolled back
         */
        void rollback();
    }

    private static long getPropertyOrDefault(String propertyName, long defaultValue) {
        long temp;
        return (temp=Apl.getIntProperty(propertyName)) != 0 ? temp : defaultValue;
    }
}
