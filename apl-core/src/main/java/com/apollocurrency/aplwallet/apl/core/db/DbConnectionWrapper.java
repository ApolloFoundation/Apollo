package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * Connection wrapper pinned to ThreadLocal with caches
 */
public class DbConnectionWrapper extends FilteredConnection {

    private ThreadLocal<DbConnectionWrapper> localConnection;
    private ThreadLocal<Map<String, Map<DbKey,Object>>> transactionCaches;
    private ThreadLocal<Set<TransactionCallback>> transactionCallback;

    long txStart = 0;

    public DbConnectionWrapper(Connection con, FilteredFactoryImpl factory, ThreadLocal<DbConnectionWrapper> localConnection,
                               ThreadLocal<Map<String, Map<DbKey,Object>>> transactionCaches,
                               ThreadLocal<Set<TransactionCallback>> transactionCallback) {
        super(con, factory);
        this.localConnection = localConnection;
        this.transactionCaches = transactionCaches;
        this.transactionCallback = transactionCallback;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new UnsupportedOperationException("Use DatabaseManager.begin() to start a new transaction");
    }

    @Override
    public void commit() throws SQLException {
        if (localConnection.get() == null) {
            super.commit();
        } else if (this != localConnection.get()) {
            throw new IllegalStateException("Previous connection not committed");
        } else {
            // repeated commit() functionality
            DbConnectionWrapper con = localConnection.get();
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
    }

    public void doCommit() throws SQLException {
        super.commit();
    }

    @Override
    public void rollback() throws SQLException {
        if (localConnection.get() == null) {
            super.rollback();
        } else if (this != localConnection.get()) {
            throw new IllegalStateException("Previous connection not committed");
        } else {
            // repeated rollback() functionality
            DbConnectionWrapper con = localConnection.get();
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
    }

    public void doRollback() throws SQLException {
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
