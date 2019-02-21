package com.apollocurrency.aplwallet.apl.core.db;

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