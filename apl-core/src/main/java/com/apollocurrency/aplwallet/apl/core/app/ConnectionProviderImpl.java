/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import java.sql.Connection;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.ConnectionProvider;

public class ConnectionProviderImpl implements ConnectionProvider {

    private TransactionalDataSource dataSource;

    public ConnectionProviderImpl(TransactionalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void begin() {
        dataSource.begin();
    }

    @Override
    public void rollbackTransaction(Connection connection) {
        dataSource.rollback();
    }

    @Override
    public void commitTransaction(Connection connection) {
        dataSource.commit();
    }

    @Override
    public void endTransaction(Connection connection) {
//        DatabaseManager.getDataSource().endTransaction();
    }

    @Override
    public boolean isInTransaction(Connection connection) {
        return dataSource.isInTransaction();
    }
}
