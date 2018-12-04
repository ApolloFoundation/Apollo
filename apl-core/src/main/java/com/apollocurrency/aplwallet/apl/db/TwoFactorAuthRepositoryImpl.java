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
    private static final String CONFIRMED_COLUMN_NAME = "confirmed";
    private static final String DELETE_QUERY = String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, KEY_COLUMN_NAME);
    private static final String SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME,
            KEY_COLUMN_NAME);
    private static final String INSERT_QUERY = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)", TABLE_NAME,
            KEY_COLUMN_NAME, SECRET_COLUMN_NAME, CONFIRMED_COLUMN_NAME);

    private static final String UPDATE_QUERY = String.format("UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?",
            TABLE_NAME, KEY_COLUMN_NAME, SECRET_COLUMN_NAME, CONFIRMED_COLUMN_NAME, KEY_COLUMN_NAME);

    public TwoFactorAuthRepositoryImpl(DataSource db) {
        this.db = db;
    }

    @Override
    public TwoFactorAuthEntity get(long account) {
        try (Connection con = db.getConnection()) {
            return getSecret(con, account);
        }
        catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }

    protected TwoFactorAuthEntity getSecret(Connection con, long account) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement(SELECT_QUERY)) {
            pstmt.setLong(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    TwoFactorAuthEntity entity = new TwoFactorAuthEntity();
                    entity.setAccount(rs.getLong(KEY_COLUMN_NAME));
                    entity.setSecret(rs.getBytes(SECRET_COLUMN_NAME));
                    entity.setConfirmed(rs.getBoolean(CONFIRMED_COLUMN_NAME));
                    return entity;
                } else return null;
            }
        }
    }

    @Override
    public boolean add(TwoFactorAuthEntity entity) {

        try (Connection con = db.getConnection()) {
            TwoFactorAuthEntity existingEntity = getSecret(con, entity.getAccount());

            if (existingEntity == null) {
                try (PreparedStatement pstmt = con.prepareStatement(INSERT_QUERY)) {
                    putEntityData(pstmt, entity);
                    return executeUpdate(pstmt);
                }

            } else return false;
        }
        catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }
    @Override
    public boolean update(TwoFactorAuthEntity entity) {

        try (Connection con = db.getConnection()) {
            TwoFactorAuthEntity existingEntity = getSecret(con, entity.getAccount());
            if (existingEntity != null) {
                try (PreparedStatement pstmt = con.prepareStatement(UPDATE_QUERY)) {
                    putEntityData(pstmt, entity);
                    pstmt.setLong(4, entity.getAccount());
                    return executeUpdate(pstmt);
                }
            } else {
                return false;
            }
        }
        catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }

    private void putEntityData(PreparedStatement pstmt, TwoFactorAuthEntity entity) throws SQLException {
        pstmt.setLong(1, entity.getAccount());
        pstmt.setBytes(2, entity.getSecret());
        pstmt.setBoolean(3, entity.isConfirmed());
    }

    private boolean executeUpdate(PreparedStatement pstmt) throws SQLException {
        return pstmt.executeUpdate() == 1;
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
