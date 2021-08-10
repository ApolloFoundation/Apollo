/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DbTransactionHelper {

    public static void executeInTransaction(TransactionalDataSource dataSource, TransactionOperation op) {
        TransactionalDataSource.StartedConnection startedConnection = dataSource.beginTransactionIfNotStarted();
        try {
            op.execute();
            dataSource.commit(!startedConnection.isAlreadyStarted());
        } catch (Exception e) {
            try {
                dataSource.rollback(!startedConnection.isAlreadyStarted());
            } catch (Exception re) {
                log.error("Error during db rollback", re);
            }
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
