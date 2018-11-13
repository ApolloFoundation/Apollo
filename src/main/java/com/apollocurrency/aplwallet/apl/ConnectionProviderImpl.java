/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import java.sql.Connection;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.updater.ConnectionProvider;

public class ConnectionProviderImpl implements ConnectionProvider {
    @Override
    public Connection getConnection() throws SQLException {
        return Db.getDb().getConnection();
    }

    @Override
    public Connection beginTransaction() {
        return Db.getDb().beginTransaction();
    }

    @Override
    public void rollbackTransaction(Connection connection) {
        Db.getDb().rollbackTransaction();
    }

    @Override
    public void commitTransaction(Connection connection) {
        Db.getDb().commitTransaction();
    }

    @Override
    public void endTransaction(Connection connection) {
        Db.getDb().endTransaction();
    }

    @Override
    public boolean isInTransaction(Connection connection) {
        return Db.getDb().isInTransaction();
    }
}
