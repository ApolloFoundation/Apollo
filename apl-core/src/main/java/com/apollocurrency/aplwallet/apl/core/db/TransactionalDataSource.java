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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import net.sf.log4jdbc.ConnectionSpy;
import org.slf4j.Logger;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Data source with Transaction support implemented by ThreadLocal connection management.
 * That class should be used only by retrieving from {@link DatabaseManager}. Should not be retrieved from CDI directly.
 */
@Vetoed
public class TransactionalDataSource extends DataSourceWrapper implements TableCache, TransactionManagement {
    private static final Logger log = getLogger(TransactionalDataSource.class);

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesHolder;
    private static FilteredFactoryImpl factory;

    private static long stmtThreshold;
    private static long txThreshold;
    private static long txInterval;
    private static boolean enableSqlLogs;

    private final ThreadLocal<DbConnectionWrapper> localConnection = new ThreadLocal<>();
    private final ThreadLocal<Map<String,Map<DbKey,Object>>> transactionCaches = new ThreadLocal<>();
    private final ThreadLocal<Set<TransactionCallback>> transactionCallback = new ThreadLocal<>();

    private volatile long txTimes = 0;
    private volatile long txCount = 0;
    private volatile long statsTime = 0;

    /**
     * Created by CDI with previously initialized properties.
     * @param dbProperties main db properties
     * @param propertiesHolder the rest of properties
     */
    @Inject
    public TransactionalDataSource(DbProperties dbProperties, PropertiesHolder propertiesHolder) {
        super(dbProperties);
        TransactionalDataSource.propertiesHolder = propertiesHolder;
        stmtThreshold = getPropertyOrDefault("apl.statementLogThreshold", 1000);
        txThreshold = getPropertyOrDefault("apl.transactionLogThreshold", 5000);
        txInterval = getPropertyOrDefault("apl.transactionLogInterval", 15) * 60 * 1000;
        enableSqlLogs = TransactionalDataSource.propertiesHolder.getBooleanProperty("apl.enableSqlLogs");
        factory = new FilteredFactoryImpl(stmtThreshold);
    }

    /**
     * Return Connection from ThreadLocal or create new one. AUTO COMMIT = TRUE for such db connection.
     * @return db connection with autoCommit = true
     * @throws SQLException possible ini exception
     */
    @Override
    public Connection getConnection() throws SQLException {
        Connection con = localConnection.get();
        if (con != null /*&& !con.isClosed() && !super.getConnection().isClosed()*/) {
            return enableSqlLogs ? new ConnectionSpy(con) : con;
        }
        DbConnectionWrapper realConnection = new DbConnectionWrapper(super.getConnection(), factory,
                localConnection, transactionCaches, transactionCallback);
        return enableSqlLogs ? new ConnectionSpy(realConnection) : realConnection;
    }

    /**
     * Optional
     * @param doSqlLog dump sql
     * @return spied db connection
     * @throws SQLException
     */
    public Connection getConnection(boolean doSqlLog) throws SQLException {
        if (!enableSqlLogs && doSqlLog) {
            return new ConnectionSpy(getConnection());
        }
        return getConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInTransaction() {
        return localConnection.get() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = getPooledConnection();
            con.setAutoCommit(false);
            con = new DbConnectionWrapper(con, factory, localConnection, transactionCaches, transactionCallback);
            ((DbConnectionWrapper)con).txStart = System.currentTimeMillis();
            localConnection.set((DbConnectionWrapper)con);
            transactionCaches.set(new HashMap<>());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() {
        this.commit(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit(boolean closeConnection) {
        DbConnectionWrapper con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doCommit();
            cleanupTransactionCallback(TransactionCallback::commit);
        } catch (SQLException e) {
            log.error("Commit data error with close = '{}'", closeConnection, e);
            throw new RuntimeException(e.toString(), e);
        } finally {
            if (closeConnection) {
                endTransaction();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback() {
        this.rollback(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback(boolean closeConnection) {
        DbConnectionWrapper con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        try {
            con.doRollback();
        } catch (SQLException e) {
            log.error("Rollback data error with close = '{}'", closeConnection, e);
            throw new RuntimeException(e.toString(), e);
        } finally {
            transactionCaches.get().clear();
            cleanupTransactionCallback(TransactionCallback::rollback);
            if (closeConnection) {
                endTransaction();
            }
        }
    }

    private void cleanupTransactionCallback(Consumer<TransactionCallback> consumer) {
        Set<TransactionCallback> callbacks = transactionCallback.get();
        if (callbacks != null) {
            callbacks.forEach(consumer);
            transactionCallback.set(null);
        }
    }

    /**
     * internal resources clean up.
     */
    private void endTransaction() {
        Connection con = localConnection.get();
        if (con == null) {
            throw new IllegalStateException("Not in transaction");
        }
        localConnection.set(null);
        transactionCaches.set(null);
        long now = System.currentTimeMillis();
        long elapsed = now - ((DbConnectionWrapper)con).txStart;
        if (elapsed >= txThreshold) {
            logThreshold(String.format("Database transaction required %.3f seconds",
                                       (double)elapsed/1000.0/*, blockchain.getHeight()*/));
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
                log.debug(String.format("Average database transaction time is %.3f seconds",
                                                     (double)times/1000.0/(double)count));
        }
        DbUtils.close(con);
    }

    /**
     * Used by FullTestSearch triggers
     * @param callback will be called later
     */
    public void registerCallback(TransactionCallback callback) {
        Set<TransactionCallback> callbacks = transactionCallback.get();
        if (callbacks == null) {
            callbacks = new HashSet<>();
            transactionCallback.set(callbacks);
        }
        callbacks.add(callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<DbKey,Object> getCache(String tableName) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache(String tableName) {
        Map<DbKey,Object> cacheMap = transactionCaches.get().get(tableName);
        if (cacheMap != null) {
            cacheMap.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        log.debug(sb.toString());
    }

    private static long getPropertyOrDefault(String propertyName, long defaultValue) {
        long temp;
        return (temp=propertiesHolder.getIntProperty(propertyName)) != 0 ? temp : defaultValue;
    }
}
