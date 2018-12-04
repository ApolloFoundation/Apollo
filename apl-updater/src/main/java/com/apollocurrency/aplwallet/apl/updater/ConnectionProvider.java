/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider {
    Connection getConnection() throws SQLException;

    Connection beginTransaction();

    void rollbackTransaction(Connection connection);

    void commitTransaction(Connection connection);

    void endTransaction(Connection connection);

    boolean isInTransaction(Connection connection);
}
