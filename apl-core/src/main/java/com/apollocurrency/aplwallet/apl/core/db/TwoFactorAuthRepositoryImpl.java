/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.exception.DbException;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TwoFactorAuthRepositoryImpl implements TwoFactorAuthRepository {

    private static final String TABLE_NAME = "two_factor_auth";
    private static final String KEY_COLUMN_NAME = "account";
    private static final String SECRET_COLUMN_NAME = "secret";
    private static final String CONFIRMED_COLUMN_NAME = "confirmed";
    private static final String DELETE_QUERY = String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, KEY_COLUMN_NAME);
    private static final String SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME,
        KEY_COLUMN_NAME);
    private static final String SELECT_QUERY_ALL = String.format("SELECT * FROM %s", TABLE_NAME);
    private static final String INSERT_QUERY = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)", TABLE_NAME,
        KEY_COLUMN_NAME, SECRET_COLUMN_NAME, CONFIRMED_COLUMN_NAME);
    private static final String UPDATE_QUERY = String.format("UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?",
        TABLE_NAME, KEY_COLUMN_NAME, SECRET_COLUMN_NAME, CONFIRMED_COLUMN_NAME, KEY_COLUMN_NAME);
    private DataSource db;

    public TwoFactorAuthRepositoryImpl(DataSource db) {
        this.db = db;
    }

    @Override
    public TwoFactorAuthEntity get(long account) {
        try (Connection con = db.getConnection()) {
            return getSecret(con, account);
        } catch (SQLException e) {
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
                log.trace("add 2fa = {}", entity);
                try (PreparedStatement pstmt = con.prepareStatement(INSERT_QUERY)) {
                    putEntityData(pstmt, entity);
                    return executeUpdate(pstmt);
                }
            } else return false;
        } catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }

    @Override
    public boolean update(TwoFactorAuthEntity entity) {
        try (Connection con = db.getConnection()) {
            TwoFactorAuthEntity existingEntity = getSecret(con, entity.getAccount());
            if (existingEntity != null) {
                log.trace("update 2fa = {}", entity);
                try (PreparedStatement pstmt = con.prepareStatement(UPDATE_QUERY)) {
                    putEntityData(pstmt, entity);
                    pstmt.setLong(4, entity.getAccount());
                    return executeUpdate(pstmt);
                }
            } else {
                return false;
            }
        } catch (SQLException e) {
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
        log.trace("delete 2fa : account = {}", account);
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(DELETE_QUERY)) {
            pstmt.setLong(1, account);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
    }

    @Override
    public List<TwoFactorAuthEntity> selectAll() {
        log.trace("start selectAll 2fa records...");
        List<TwoFactorAuthEntity> result = new ArrayList<>(10);
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(SELECT_QUERY_ALL)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TwoFactorAuthEntity entity = new TwoFactorAuthEntity();
                    entity.setAccount(rs.getLong(KEY_COLUMN_NAME));
                    entity.setSecret(rs.getBytes(SECRET_COLUMN_NAME));
                    entity.setConfirmed(rs.getBoolean(CONFIRMED_COLUMN_NAME));
                    result.add(entity);
                }
            }
        } catch (SQLException e) {
            throw new DbException(e.toString(), e);
        }
        log.debug("selectAll found 2fa records = [{}]", result.size());
        return result;
    }
}
