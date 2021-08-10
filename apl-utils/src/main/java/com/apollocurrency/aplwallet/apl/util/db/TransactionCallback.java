/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

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