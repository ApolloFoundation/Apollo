/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;

public class DbTransactionHelper {

    public static void executeInTransaction(TransactionalDataSource dataSource, TransactionOperation op) {
        TransactionalDataSource.StartedConnection startedConnection = dataSource.beginTransactionIfNotStarted();
        try {
            op.execute();
            dataSource.commit(!startedConnection.isAlreadyStarted());
        } catch (Exception e) {
            dataSource.rollback(!startedConnection.isAlreadyStarted());
            throw new DbTransactionExecutionException(e.toString(), e);
        }
    }

    public static<T> T executeInTransaction(TransactionalDataSource dataSource, TransactionFunction<T> op) {
        TransactionalDataSource.StartedConnection startedConnection = dataSource.beginTransactionIfNotStarted();
        try {
            T result = op.execute();
            dataSource.commit(!startedConnection.isAlreadyStarted());
            return result;
        } catch (Exception e) {
            dataSource.rollback(!startedConnection.isAlreadyStarted());
            throw new DbTransactionExecutionException(e.toString(), e);
        }
    }
    public static<T> T executeInConnection(TransactionalDataSource dataSource, TransactionFunction<T> op) {
        TransactionalDataSource.StartedConnection startedConnection = dataSource.beginTransactionIfNotStarted();
        try {
            T result = op.execute();
            if (!startedConnection.isAlreadyStarted()) {
                dataSource.commit(true);
            }
            return result;
        } catch (Exception e) {
            dataSource.rollback(!startedConnection.isAlreadyStarted());
            throw new DbTransactionExecutionException(e.toString(), e);
        }
    }
    public interface TransactionOperation {
        void execute() throws Exception;
    }

    public interface TransactionFunction<T> {
        T execute() throws Exception;
    }

    public static class DbTransactionExecutionException extends RuntimeException {
        public DbTransactionExecutionException(String message) {
            super(message);
        }

        public DbTransactionExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
