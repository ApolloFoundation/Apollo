/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;

public class TransactionHelper {

    public static void executeInTransaction(TransactionalDataSource dataSource, TransactionOperation op) {
        TransactionalDataSource.StartedConnection startedConnection = dataSource.beginTransactionIfNotStarted();
        try {
            op.execute();
            dataSource.commit(!startedConnection.isAlreadyStarted());
        } catch (Exception e) {
            dataSource.rollback(!startedConnection.isAlreadyStarted());
        }
    }
    public interface TransactionOperation {
        void execute() throws Exception;
    }
}
