/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class OptionDAO {
    private DatabaseManager databaseManager;

    @Inject
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
            log.error(e.getMessage());
        }
        return null;
    }

    public boolean set(String optionName, String optionValue) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!exist(optionName)) {
            try (Connection con = dataSource.getConnection()) {
                PreparedStatement stmt = con.prepareStatement("INSERT INTO option (name, \"VALUE\") VALUES (?, ?)");
                stmt.setString(1, optionName);
                stmt.setString(2, optionValue);
                stmt.execute();
            }
            catch (SQLException e) {
                log.error("OptionDAO insert error, {}={}, {}", optionName, optionValue, e.getMessage());
            }
        } else {
            try (Connection con = dataSource.getConnection()) {
                PreparedStatement stmt = con.prepareStatement("UPDATE option set value = ? WHERE name = ?");
                stmt.setString(1, optionValue);
                stmt.setString(2, optionName);
                stmt.execute();
            }
            catch (SQLException e) {
                log.error("OptionDAO update error, {}={}, {}", optionName, optionValue, e.getMessage());
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
                log.error(e.getMessage());
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
                log.error(e.getMessage());
            }
    }

    public boolean exist(String optionKey) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement stmt = con.prepareStatement("SELECT count(*) FROM option WHERE name = ?");
            stmt.setString(1, optionKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        catch (SQLException e) {
            log.error(e.getMessage());
        }
        return false;
    }

}
