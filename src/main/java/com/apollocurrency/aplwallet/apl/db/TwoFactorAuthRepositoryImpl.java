/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.util.exception.DbException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TwoFactorAuthRepositoryImpl implements TwoFactorAuthRepository {

    private DataSource db;
    private static final String TABLE_NAME = "two_factor_auth";
    private static final String KEY_COLUMN_NAME = "account";
    private static final String SECRET_COLUMN_NAME = "secret";
    private static final String DELETE_QUERY = String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, KEY_COLUMN_NAME);
    private static final String SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME,
            KEY_COLUMN_NAME);
    private static final String INSERT_QUERY = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)", TABLE_NAME,
            KEY_COLUMN_NAME, SECRET_COLUMN_NAME);

    public TwoFactorAuthRepositoryImpl(DataSource db) {
        this.db = db;
    }

    @Override
    public byte[] getSecret(long account) {
        try (Connection con = db.getConnection()) {
            return getSecret(con, account);
        }
        catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }

    protected byte[] getSecret(Connection con, long account) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement(SELECT_QUERY)) {
            pstmt.setLong(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes(SECRET_COLUMN_NAME);
                } else return null;
            }
        }
    }

    @Override
    public boolean saveSecret(long account, byte[] secret) {
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(INSERT_QUERY)) {
            pstmt.setLong(1, account);
            pstmt.setBytes(2, secret);
            if (getSecret(con, account) == null) {
                pstmt.executeUpdate();
                return true;
            } else {
                return false;
            }
        }
        catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }

    @Override
    public boolean delete(long account) {
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(DELETE_QUERY)) {
            pstmt.setLong(1, account);
            return pstmt.executeUpdate() > 0;
        }
        catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }
}
