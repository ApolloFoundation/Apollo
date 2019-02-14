package com.apollocurrency.aplwallet.apl.core.db;

public interface TransactionManagement {

    /**
     * Check if underlying db connection exists in current ThreadLocal
     * @return true if connection exists, false otherwise
     */
    boolean isInTransaction();

    /**
     * Start transaction using existing underlying db connection
     */
    void begin();

    /**
     * Commit transaction data using existing underlying db connection, do NOT close connection
     */
    void commit();

    /**
     * Commit transaction data using existing underlying db connection.
     * @param closeConnection true if db connection should be closed, false otherwise
     */
    void commit(boolean closeConnection);

    /**
     * Roolback transaction data using existing underlying db connection.
     */
    void rollback();

    /**
     * Roolback transaction data using existing underlying db connection.
     * @param closeConnection true if db connection should be closed, false otherwise
     */
    void rollback(boolean closeConnection);

//    void endTransaction();

}
