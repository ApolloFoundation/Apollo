/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.updater.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionProviderImpl implements ConnectionProvider {
    @Override
    public Connection getConnection() throws SQLException {
        return Db.db.getConnection();
    }

    @Override
    public Connection beginTransaction() {
        return Db.db.beginTransaction();
    }

    @Override
    public void rollbackTransaction(Connection connection) {
        Db.db.rollbackTransaction();
    }

    @Override
    public void commitTransaction(Connection connection) {
        Db.db.commitTransaction();
    }

    @Override
    public void endTransaction(Connection connection) {
        Db.db.endTransaction();
    }

    @Override
    public boolean isInTransaction(Connection connection) {
        return Db.db.isInTransaction();
    }
}
