/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

public class UpdaterDb {
    private static final Logger LOG = getLogger(UpdaterDb.class);

    private static final RuntimeException INCONSISTENT_UPDATE_STATUS_TABLE_EXCEPTION = new RuntimeException(
            "(\'update_status\' table is inconsistent. (more than one update transaction " +
            "present)");
    public static Transaction loadLastUpdateTransaction() {
        try (Connection connection = Db.db.getConnection())
        {
            return loadLastUpdateTransaction(connection);
        }
        catch (SQLException e) {
            LOG.error("Db error", e);
        }
        return null;
    }
    public static Transaction loadLastUpdateTransaction(Connection connection) {
            Transaction updateTransaction = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM transaction where id = (SELECT transaction_id FROM update_status)")) {
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                updateTransaction = TransactionDb.loadTransaction(connection, rs);
            }
            checkInconsistency(rs);
        }
        catch (SQLException | AplException.NotValidException e) {
            LOG.debug("Unable to load update transaction", e);
        }
        return updateTransaction;
    }

    private static void checkInconsistency(ResultSet rs) throws SQLException {
        if (rs.next()) {
            throw INCONSISTENT_UPDATE_STATUS_TABLE_EXCEPTION;
        }
    }

    private static void checkInconsistency(int updatedRows) {
        if (updatedRows > 1) {
            throw INCONSISTENT_UPDATE_STATUS_TABLE_EXCEPTION;
        }
    }

    //required operations in one transaction
    public static boolean clearAndSaveUpdateTransaction(Long transactionId) {
        boolean success = false;
        boolean isInTransaction = Db.db.isInTransaction();
        try {
            Connection connection;
            if (!isInTransaction) connection = Db.db.beginTransaction();
            else connection = Db.db.getConnection();
            clear(connection);
            success = saveUpdateTransaction(transactionId, connection);
            Db.db.commitTransaction();
            success = true;            
        }
        catch (SQLException e) {
            Db.db.rollbackTransaction();
            LOG.error("Db error", e);
        }
        finally {
            if (!isInTransaction) Db.db.endTransaction();
            return success;
        }
        
    }

    public static boolean saveUpdateTransaction(Long transactionId) {
        try (Connection connection = Db.db.getConnection()) {
            return saveUpdateTransaction(transactionId, connection);
        }
        catch (SQLException e) {
            LOG.debug("Db error! ", e);
            return false;
        }
    }

    public static boolean saveUpdateTransaction(Long transactionId, Connection connection) {
        int updateCount;
        try(PreparedStatement statement = connection.prepareStatement("INSERT INTO update_status (transaction_id) VALUES (?)")) {
            statement.setLong(1, transactionId);
            updateCount = statement.executeUpdate();
        }
        catch (SQLException e) {
            LOG.debug("Unable to insert update transaction id! ", e);
            return false;
        }
        checkInconsistency(updateCount);
        return updateCount == 1;
    }


    public static boolean saveUpdateStatus(Boolean status) {
        int updateCount;
        try (Connection connection = Db.db.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE update_status set updated = ?")) {
            statement.setBoolean(1, status);
            updateCount = statement.executeUpdate();
        }
        catch (SQLException e) {
            LOG.debug("Unable to insert update status! ", e);
            return false;
        }
        checkInconsistency(updateCount);
        return updateCount == 1;
    }

    public static boolean getUpdateStatus() {
        try (Connection connection = Db.db.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT updated FROM update_status");
             ResultSet rs =  statement.executeQuery()) {
            if (rs.next()) {
                return rs.getBoolean("updated");
            }
            checkInconsistency(rs);
        }
        catch (SQLException e) {
            LOG.debug("Unable to get update status! ", e);
        }
        return false;
    }

    public static int clear() {
        try (Connection connection = Db.db.getConnection()) {
            return clear(connection);
        }
        catch (SQLException e) {
            LOG.debug("Db error ", e);
            return 0;
        }
    }

    public static int clear(Connection connection) {
        int deletedTransactionCount;
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM update_status")) {
            deletedTransactionCount = statement.executeUpdate();
        }
        catch (SQLException e) {
            LOG.debug("Unable to delete db entries! ", e);
            return 0;
        }
        return deletedTransactionCount;
    }
}
