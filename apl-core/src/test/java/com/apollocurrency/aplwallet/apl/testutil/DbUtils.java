/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

public class DbUtils {

    public static void inTransaction(DbExtension extension, Consumer<Connection> consumer) {
        inTransaction(extension.getDatabaseManager(), consumer);
    }

    public static void inTransaction(DatabaseManager manager, Consumer<Connection> consumer) {
        inTransaction(manager.getDataSource(), consumer);
    }

    public static void inTransaction(TransactionalDataSource dataSource, Consumer<Connection> consumer) {
        try (Connection con = dataSource.begin()) { // start new transaction
            consumer.accept(con);
            dataSource.commit();
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e);
        }
    }

    public static <T> T getInTransaction(DbExtension extension, Function<Connection, T> function) {
        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();
        try (Connection con = dataSource.begin()) { // start new transaction
            T res = function.apply(con);
            dataSource.commit();
            return res;
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e);
        }
    }
}
