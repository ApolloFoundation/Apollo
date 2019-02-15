/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.repository;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.UpdateTransaction;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.slf4j.Logger;

public class UpdaterDbRepository implements UpdaterRepository {
    private static final Logger LOG = getLogger(UpdaterDbRepository.class);

    private static final RuntimeException INCONSISTENT_UPDATE_STATUS_TABLE_EXCEPTION = new RuntimeException(
            "(\'update_status\' table is inconsistent. (more than one update transaction " +
                    "present)");
    private UpdaterMediator updaterMediator;
    private TransactionalDataSource dataSource;

    @Inject
    public UpdaterDbRepository(UpdaterMediator updaterMediator) {
        this.updaterMediator = updaterMediator;
        this.dataSource = updaterMediator.getDataSource();
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

    @Override
    public UpdateTransaction getLast() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM update_status LEFT JOIN transaction on update_status.transaction_id = transaction.id")) {
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                Transaction tr = updaterMediator.loadTransaction(connection, rs);
                boolean updated = rs.getBoolean("updated");
                checkInconsistency(rs);
                return new UpdateTransaction(tr, updated);
            }
        }
        catch (SQLException | AplException.NotValidException e) {
            LOG.debug("Unable to load update transaction", e);
            throw new RuntimeException(e.toString(), e);
        }
        return null;
    }

    @Override
    public void save(UpdateTransaction transaction) {
        try (Connection connection = dataSource.getConnection()) {
            save(connection, transaction);
        }
        catch (SQLException e) {
            LOG.error("Unable to get connection", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void save(Connection connection, UpdateTransaction transaction) {

        try (PreparedStatement pstm = connection.prepareStatement("INSERT INTO update_status (transaction_id, updated) values (?, ?)")) {
            pstm.setLong(1, transaction.getTransaction().getId());
            pstm.setBoolean(2, transaction.isUpdated());
            int i = pstm.executeUpdate();
            if (i != 1) {
                throw new RuntimeException("Expected 1 saved record, got " + i);
            }
        }
        catch (SQLException e) {
            LOG.error("Unable to save update transaction", e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void update(UpdateTransaction transaction) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstm = connection.prepareStatement("UPDATE update_status SET updated = ? WHERE transaction_id = ?")) {
            pstm.setBoolean(1, transaction.isUpdated());
            pstm.setLong(2, transaction.getTransaction().getId());
        }
        catch (SQLException e) {
            LOG.error(e.toString(), e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int clear(Connection connection) {
        int deletedTransactionCount;
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM update_status")) {
            deletedTransactionCount = statement.executeUpdate();
        }
        catch (SQLException e) {
            LOG.error("Unable to delete db entries! ", e);
            return 0;
        }
        return deletedTransactionCount;
    }

    @Override
    public void clearAndSave(UpdateTransaction transaction) {
        boolean isInTransaction = dataSource.isInTransaction();
        Connection con = null;
        try {
            con = dataSource.getConnection();
            if (!isInTransaction) dataSource.begin();
            else con = dataSource.getConnection();
            clear(con);
            save(con, transaction);
            dataSource.commit();
        }
        catch (SQLException e) {
            dataSource.rollback();
            LOG.error(e.toString(), e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int clear() {
        try (Connection connection = dataSource.getConnection()) {
            return clear(connection);
        }
        catch (SQLException e) {
            LOG.error("Unable to open connection" + e.toString());
            throw new RuntimeException(e.toString(), e);
        }
    }
}
