package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UpdaterDb {

    public static Transaction loadLastUpdateTransaction() {
        try (Connection connection = Db.db.getConnection())
        {
            return loadLastUpdateTransaction(connection);
        }
        catch (SQLException e) {
            Logger.logErrorMessage("Db error", e);
        }
        return null;
    }
    public static Transaction loadLastUpdateTransaction(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM transaction where id = (SELECT transaction_id FROM update_status)")) {
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return TransactionDb.loadTransaction(connection, rs);
            }
        }
        catch (SQLException | AplException.NotValidException e) {
            Logger.logDebugMessage("Unable to load update transaction", e);
        }
         return null;
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
            Logger.logErrorMessage("Db error", e);
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
            Logger.logDebugMessage("Db error! ", e);
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
            Logger.logDebugMessage("Unable to insert update transaction id! ", e);
            return false;
        }
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
            Logger.logDebugMessage("Unable to insert update status! ", e);
            return false;
        }
        return updateCount == 1;
    }

    public static boolean getUpdateStatus() {
        try (Connection connection = Db.db.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT updated FROM update_status")) {
            ResultSet rs =  statement.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("updated");
            }
        }
        catch (SQLException e) {
            Logger.logDebugMessage("Unable to get update status! ", e);
        }
        return false;
    }

    public static int clear() {
        try (Connection connection = Db.db.getConnection()) {
            return clear(connection);
        }
        catch (SQLException e) {
            Logger.logDebugMessage("Db error ", e);
            return 0;
        }
    }

    public static int clear(Connection connection) {
        int deletedTransactionCount;
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM update_status")) {
            deletedTransactionCount = statement.executeUpdate();
        }
        catch (SQLException e) {
            Logger.logDebugMessage("Unable to delete db entries! ", e);
            return 0;
        }
        return deletedTransactionCount;
    }
}
