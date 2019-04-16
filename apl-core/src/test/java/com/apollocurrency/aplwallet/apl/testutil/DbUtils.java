/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

public class DbUtils {
    public static void inTransaction(DbExtension extension, Consumer<Connection> consumer) throws SQLException {
        TransactionalDataSource dataSource = extension.getDatabaseManger().getDataSource();
        try (Connection con = dataSource.begin()) { // start new transaction
            consumer.accept(con);
            dataSource.commit();
        }
        catch (SQLException e) {
            dataSource.rollback();
            throw e;
        }
    }
}
