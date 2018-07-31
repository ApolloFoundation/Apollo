package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdaterDb {

    public static Transaction loadLastUpdateTransaction() {
        try (Connection connection = Db.db.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM transaction where id = (SELECT transaction_id FROM update_status)")) {
            return new DbIterator<Transaction>(connection, statement, TransactionDb::loadTransaction).next();
        }
        catch (SQLException e) {
            Logger.logDebugMessage("Unable to load update transaction", e);
            return null;
        }
    }

    public static boolean saveUpdateTransaction(Long transactionId) {
        int updateCount;
        try (Connection connection = Db.db.getConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO update_status (transaction_id) VALUES (?)")) {
            statement.setLong(1, transactionId);
            updateCount = statement.executeUpdate();
        }
        catch (SQLException e) {
            Logger.logDebugMessage("Unable to insert update transaction id! ", e);
            return false;
        }
        return updateCount == 1;
    }

    public static boolean setUpdateStatus(Boolean status) {
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
        boolean status;
        try (Connection connection = Db.db.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT updated FROM update_status")) {
            status = statement.executeQuery().getBoolean("updated");
        }
        catch (SQLException e) {
            Logger.logDebugMessage("Unable to insert update status! ", e);
            return false;
        }
        return status;
    }

    public static int clear() {
        int deletedTransactionCount;
        try (Connection connection = Db.db.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM update_status")) {
             deletedTransactionCount = statement.executeUpdate();
        }
        catch (SQLException e) {
            Logger.logDebugMessage("Unable to delete db entries! ", e);
            return 0;
        }
        return deletedTransactionCount;
    }
}
