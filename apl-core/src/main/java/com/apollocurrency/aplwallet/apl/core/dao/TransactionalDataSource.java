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

package com.apollocurrency.aplwallet.apl.core.dao;

import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;
import com.apollocurrency.aplwallet.apl.core.db.DbConnectionWrapper;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.FilteredFactoryImpl;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.TransactionCallback;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Data;
import net.sf.log4jdbc.ConnectionSpy;
import org.slf4j.Logger;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Data source with Transaction support implemented by ThreadLocal connection management.
 * That class should be used only by retrieving from {@link DatabaseManager}. Should not be retrieved from CDI directly.
 */
@Vetoed
public class TransactionalDataSource extends DataSourceWrapper implements TransactionManagement {
    private static final Logger log = getLogger(TransactionalDataSource.class);
    private final ThreadLocal<DbConnectionWrapper> localConnection = new ThreadLocal<>();
    private final ThreadLocal<Set<TransactionCallback>> transactionCallback = new ThreadLocal<>();
    // TODO: YL remove static instance later
    private FilteredFactoryImpl factory;
    private long txThreshold;
    private long txInterval;
    private boolean enableSqlLogs;
    private volatile long txTimes = 0;
    private volatile long txCount = 0;
    private volatile long statsTime = 0;
    /**
     * Optional.EMPTY for 'main db', Optional.value apl_blockchain_XXXXXX_shard_Y for shardId (chainId = XXX, shard Id = Y)
     */
    private Optional<String> dbIdentity;

    /**
     * Created by CDI with previously initialized properties.
     *
     * @param dbProperties     main db properties
     * @param propertiesHolder the rest of properties
     */
    @Inject
    public TransactionalDataSource(DbProperties dbProperties, PropertiesHolder propertiesHolder) {
        this(dbProperties,
            propertiesHolder.getIntProperty("apl.statementLogThreshold", 1000),
            propertiesHolder.getIntProperty("apl.transactionLogThreshold", 5000),
            propertiesHolder.getIntProperty("apl.transactionLogInterval", 15) * 60 * 1000,
            propertiesHolder.getBooleanProperty("apl.enableSqlLogs", false));
    }

    public TransactionalDataSource(DbProperties dbProperties, int stmtThreshold, int txThreshold, int txInterval, boolean enableSqlLogs) {
        super(dbProperties);
        this.txThreshold = txThreshold;
        this.txInterval = txInterval;
        this.enableSqlLogs = enableSqlLogs;
        this.factory = new FilteredFactoryImpl(stmtThreshold);
        this.dbIdentity = dbProperties.getDbIdentity();
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

    /**
     * Return Connection from ThreadLocal or create new one. AUTO COMMIT = TRUE for such db connection.
     *
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
            localConnection, transactionCallback);
        return enableSqlLogs ? new ConnectionSpy(realConnection) : realConnection;
    }

    /**
     * Optional
     *
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
    public Connection begin() {
        if (localConnection.get() != null) {
            throw new IllegalStateException("Transaction already in progress");
        }
        try {
            Connection con = getPooledConnection();
            con.setAutoCommit(false);
            DbConnectionWrapper wcon = new DbConnectionWrapper(con, this.factory, this.localConnection, this.transactionCallback);
            wcon.setTxStart(System.currentTimeMillis());
            localConnection.set(wcon);
            return wcon;
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
            cleanupTransactionCallback(TransactionCallback::rollback);
            if (closeConnection) {
                endTransaction();
            }
        }
    }

    private void cleanupTransactionCallback(Consumer<TransactionCallback> consumer) {
        Set<TransactionCallback> callbacks = this.transactionCallback.get();
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
        long now = System.currentTimeMillis();
        long elapsed = now - ((DbConnectionWrapper) con).getTxStart();
        if (elapsed >= txThreshold) {
            logThreshold(String.format("Database transaction required %.3f seconds",
                (double) elapsed / 1000.0/*, blockchain.getHeight()*/));
        } else {
            long count, times;
            boolean logStats = false;
            synchronized (this) {
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
                    (double) times / 1000.0 / (double) count));
        }
        DbUtils.close(con);
    }

    /**
     * Used by FullTestSearch triggers
     *
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
     * Return db identity value related to database type - main db, shard db.
     * main db examples : Optional.EMPTY
     * shard db examples: Optional.apl_blockchain_b5d7b6_shard_1, Optional.apl_blockchain_2f2b61_shard_4
     *
     * @return Optional.EMPTY for main db, Optional.value for shardId
     */
    public Optional<String> getDbIdentity() {
        return this.dbIdentity;
    }


    public StartedConnection beginTransactionIfNotStarted() {
        if (isInTransaction()) {
            return new StartedConnection(localConnection.get(), true);
        } else {
            return new StartedConnection(begin(), false);
        }
    }
    @Data
    public static class StartedConnection {
        private final Connection connection;
        private final boolean alreadyStarted;
    }
}
