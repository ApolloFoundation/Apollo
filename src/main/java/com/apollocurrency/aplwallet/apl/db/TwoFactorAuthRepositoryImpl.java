/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.AlreadyExistsException;
import com.apollocurrency.aplwallet.apl.NotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TwoFactorAuthRepositoryImpl implements TwoFactorAuthRepository {

    private BasicDb db;
    private static final String TABLE_NAME = "two_factor_auth";
    private static final String KEY_COLUMN_NAME = "account";
    private static final String SECRET_COLUMN_NAME = "secret";
    private static final String DELETE_QUERY = String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, KEY_COLUMN_NAME);
    private static final String SELECT_QUERY = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME,
            KEY_COLUMN_NAME);
    private static final String INSERT_QUERY = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)", TABLE_NAME,
            KEY_COLUMN_NAME, SECRET_COLUMN_NAME);

    public TwoFactorAuthRepositoryImpl(BasicDb db) {
        this.db = db;
    }

    @Override
    public byte[] getSecret(long account) throws NotFoundException {
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(SELECT_QUERY)) {
            pstmt.setLong(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] key = rs.getBytes(SECRET_COLUMN_NAME);
                    return key;
                } else throw new NotFoundException("Account has not 2fa");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void saveSecret(long account, byte[] secret) throws AlreadyExistsException {
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(INSERT_QUERY)) {
            pstmt.setLong(1, account);
            pstmt.setBytes(2, secret);

            try {
                int i = pstmt.executeUpdate();
                if (i != 1) {
                    throw new RuntimeException("Insert error: expected: 1 " + " but got " + i);
                }
            }
            catch (SQLException e) {
                throw new AlreadyExistsException("Account is already in db", e.getCause());
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void delete(long account) throws NotFoundException {
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(DELETE_QUERY)) {
            pstmt.setLong(1, account);
            int i = pstmt.executeUpdate();
            if (i > 1) {
                throw new RuntimeException("Delete error: expected: 1 " + " but got " + i);
            } else if (i == 0) {
                throw new NotFoundException("Not found account in db");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
