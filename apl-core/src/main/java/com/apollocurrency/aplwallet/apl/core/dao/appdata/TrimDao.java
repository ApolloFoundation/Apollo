/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TrimEntry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Singleton
public class TrimDao {
    private DatabaseManager databaseManager;

    @Inject
    public TrimDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public TrimEntry save(TrimEntry trimEntry) {
        try (Connection con = databaseManager.getDataSource().getConnection()) {
            if (trimEntry.getId() != null) {
                try (PreparedStatement pstm = con.prepareStatement("UPDATE trim SET height = ?, done = ? WHERE db_id = ?")) {
                    pstm.setInt(1, trimEntry.getHeight());
                    pstm.setBoolean(2, trimEntry.isDone());
                    pstm.setLong(3, trimEntry.getId());
                    pstm.executeUpdate();
                    return trimEntry;
                }
            } else {
                try (PreparedStatement pstm = con.prepareStatement(
                    "INSERT INTO trim (height, done) VALUES (?, ?) ",
                    Statement.RETURN_GENERATED_KEYS
                )) {
                    pstm.setInt(1, trimEntry.getHeight());
                    pstm.setBoolean(2, trimEntry.isDone());
                    pstm.executeUpdate();
                    try (ResultSet keySet = pstm.getGeneratedKeys()) {
                        if (keySet.next()) {
                            trimEntry.setId(keySet.getLong(1));
                            return trimEntry;
                        } else {
                            throw new IllegalStateException("Primary auto-increment key was not generated");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public TrimEntry get() {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trim");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return new TrimEntry(rs.getLong("db_id"), rs.getInt("height"), rs.getBoolean("done"));
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void clear() {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM trim");
        ) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int count() {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT count(*) FROM trim");
             ResultSet rs = pstmt.executeQuery()
        ) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
