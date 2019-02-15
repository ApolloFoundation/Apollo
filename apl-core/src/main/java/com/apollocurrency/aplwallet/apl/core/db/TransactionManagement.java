package com.apollocurrency.aplwallet.apl.core.db;

public interface TransactionManagement {

    /**
     * Return TRUE if there is db connection in ThreadLocal
     * @return true if connection exists in ThreadLocal, false otherwise
     */
    boolean isInTransaction();

    /**
     * Start transaction using existing underlying db connection
     * Create new connection (with autoCommit = FALSE) and put in into ThreadLocal for later usage.
     */
    void begin();

    /**
     * Commit/flash transaction data on currently opened db connection and close connection.
     * It is closed and returned into connection pool.
     */
    void commit();

    /**
     * Commit/flash transaction data using existing underlying db connection.
     * It can be closed OR can be left opened.
     * @param closeConnection true if db connection should be closed, false otherwise
     */
    void commit(boolean closeConnection);

    /**
     * Rollback transaction data using existing underlying db connection and close it.
     * It is closed and returned into connection pool.
     */
    void rollback();

    /**
     * Rollback transaction data using existing underlying db connection.
     * It can be closed OR can be left opened.
     * @param closeConnection true if db connection should be closed, false otherwise
     */
    void rollback(boolean closeConnection);

}
