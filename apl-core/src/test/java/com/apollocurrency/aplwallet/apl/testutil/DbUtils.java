/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;

import java.sql.Connection;
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
        } catch (Throwable e) {
            dataSource.rollback();
            throw new RuntimeException(e);
        }
    }
    public static void inTransactionAndRollback(TransactionalDataSource dataSource, Consumer<Connection> consumer) {
        try (Connection con = dataSource.begin()) { // start new transaction
            consumer.accept(con);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            dataSource.rollback();
        }
    }

    public static void checkAndRunInTransaction(DbExtension extension, Consumer<Connection> consumer) {
        checkAndRunInTransaction(extension.getDatabaseManager(), consumer);
    }

    public static void checkAndRunInTransaction(DatabaseManager manager, Consumer<Connection> consumer) {
        checkAndRunInTransaction(manager.getDataSource(), consumer);
    }

    public static void checkAndRunInTransaction(TransactionalDataSource dataSource, Consumer<Connection> consumer) {
        if (!dataSource.isInTransaction()) {
            try (Connection con = dataSource.begin()) { // start new transaction
                consumer.accept(con);
                dataSource.commit();
            } catch (Throwable e) {
                dataSource.rollback();
                throw new RuntimeException(e);
            }
        } else {
            try (Connection con = dataSource.getConnection()) { // take old transaction
                consumer.accept(con);
                dataSource.commit();
            } catch (Throwable e) {
                dataSource.rollback();
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> T getInTransaction(DbExtension extension, Function<Connection, T> function) {
        TransactionalDataSource dataSource = extension.getDatabaseManager().getDataSource();
        try (Connection con = dataSource.begin()) { // start new transaction
            T res = function.apply(con);
            dataSource.commit();
            return res;
        } catch (Throwable e) {
            dataSource.rollback();
            throw new RuntimeException(e);
        }
    }

    public static JdbiHandleFactory createJdbiHandleFactory(DatabaseManager databaseManager) {
        JdbiConfiguration jdbiConfiguration = new JdbiConfiguration(databaseManager);
        jdbiConfiguration.init();
        return new JdbiHandleFactory(jdbiConfiguration.jdbi());
    }
}
