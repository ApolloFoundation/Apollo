/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.BasicDb;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

public class TwoFactorAuthRepositoryImpl implements TwoFactorAuthRepository {
    private static final Logger LOG = getLogger(TwoFactorAuthRepositoryImpl.class);

    private BasicDb db;
    private static final String TABLE = "two_factor_auth";
    private static final String KEY = "account";

    public TwoFactorAuthRepositoryImpl(BasicDb db) {
        this.db = db;
    }

    @Override
    public byte[] getSecret(long account) throws NotFoundException {
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + TABLE + " WHERE " + KEY + " = ?")) {
            pstmt.setLong(1, account);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] key = rs.getBytes("secret");
                    checkConsistency(rs);
                    return key;
                } else throw new NotFoundException("Account has not 2fa");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void checkConsistency(ResultSet rs) throws SQLException {
        if (rs.next()) {
            throw new RuntimeException("Inconsistent table data, required 1 record");
        }
    }

    @Override
    public void saveSecret(long account, byte[] secret) throws AlreadyExistsException {

    }

    @Override
    public void delete(long account) throws NotFoundException {

    }
}
