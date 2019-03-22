/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import org.slf4j.Logger;

//TODO: Refactor to CDI

public class OptionDAO {
    private static final Logger LOG = getLogger(OptionDAO.class);
    private DatabaseManager databaseManager;

    public OptionDAO() {
        databaseManager = CDI.current().select(DatabaseManager.class).get();
    }

    public OptionDAO(DatabaseManager databaseManager) {
        Objects.requireNonNull(databaseManager, "Database Manager cannot be null");
        this.databaseManager = databaseManager;
    }

    public String get(String optionName) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM option WHERE name = ?");
            stmt.setString(1, optionName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        }
        catch (SQLException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    public boolean set(String optionName, String optionValue) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (get(optionName) == null) {
            try (Connection con = dataSource.getConnection()) {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO option (name, value) VALUES (?, ?)");
                stmt.setString(1, optionName);
                stmt.setString(2, optionValue);
                stmt.execute();
            }
            catch (SQLException e) {
                LOG.error("OptionDAO insert error, {}={}, {}", optionName, optionValue, e.getMessage());
            }
        } else {
            try (Connection con = dataSource.getConnection()) {
                PreparedStatement stmt = con.prepareStatement("UPDATE option set value = ? WHERE name = ?");
                stmt.setString(1, optionValue);
                stmt.setString(2, optionName);
                stmt.execute();
            }
            catch (SQLException e) {
                LOG.error("OptionDAO update error, {}={}, {}", optionName, optionValue, e.getMessage());
            }
        }
        return true;
    }

    public boolean delete(String optionName) {
        if (get(optionName) != null) {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try (Connection con = dataSource.getConnection()) {
                PreparedStatement stmt = con.prepareStatement("DELETE FROM option WHERE name = ?");
                stmt.setString(1, optionName);
                int deletedRows = stmt.executeUpdate();
                return deletedRows == 1;
            }
            catch (SQLException e) {
                LOG.error(e.getMessage());
            }
        }
        return false;
    }

    public void deleteAll() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        Objects.requireNonNull(dataSource, "dataSource is NULL");
            try (Connection con = dataSource.getConnection()) {
                PreparedStatement stmt = con.prepareStatement("DELETE FROM option");
                int deletedRows = stmt.executeUpdate();
            }
            catch (SQLException e) {
                LOG.error(e.getMessage());
            }
    }

}
