package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.service.fulltext.TransactionCallback;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * Connection wrapper pinned to ThreadLocal with caches
 */
public class DbConnectionWrapper extends FilteredConnection {

    long txStart = 0;
    private ThreadLocal<DbConnectionWrapper> localConnection;
    private ThreadLocal<Set<TransactionCallback>> transactionCallback;

    public DbConnectionWrapper(Connection con, FilteredFactoryImpl factory, ThreadLocal<DbConnectionWrapper> localConnection,
                               ThreadLocal<Set<TransactionCallback>> transactionCallback) {
        super(con, factory);
        this.localConnection = localConnection;
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

    public synchronized void setTxStart(long txStart) {
        this.txStart = txStart;
    }

    public long getTxStart() {
        return this.txStart;
    }
}
