package com.apollocurrency.aplwallet.apl.core.service.fulltext;

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