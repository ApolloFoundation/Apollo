/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public final class AccountLease {
    
    final long lessorId;
    final DbKey dbKey;
    long currentLesseeId;
    int currentLeasingHeightFrom;
    int currentLeasingHeightTo;
    long nextLesseeId;
    int nextLeasingHeightFrom;
    int nextLeasingHeightTo;

    public AccountLease(long lessorId, int currentLeasingHeightFrom, int currentLeasingHeightTo, long currentLesseeId) {
        this.lessorId = lessorId;
        this.dbKey = Account.accountLeaseDbKeyFactory.newKey(this.lessorId);
        this.currentLeasingHeightFrom = currentLeasingHeightFrom;
        this.currentLeasingHeightTo = currentLeasingHeightTo;
        this.currentLesseeId = currentLesseeId;
    }

    public AccountLease(ResultSet rs, DbKey dbKey) throws SQLException {
        this.lessorId = rs.getLong("lessor_id");
        this.dbKey = dbKey;
        this.currentLeasingHeightFrom = rs.getInt("current_leasing_height_from");
        this.currentLeasingHeightTo = rs.getInt("current_leasing_height_to");
        this.currentLesseeId = rs.getLong("current_lessee_id");
        this.nextLeasingHeightFrom = rs.getInt("next_leasing_height_from");
        this.nextLeasingHeightTo = rs.getInt("next_leasing_height_to");
        this.nextLesseeId = rs.getLong("next_lessee_id");
    }

    void save(Connection con) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_lease " + "(lessor_id, current_leasing_height_from, current_leasing_height_to, current_lessee_id, " + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, height, latest) " + "KEY (lessor_id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.lessorId);
            DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightFrom);
            DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightTo);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.currentLesseeId);
            DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightFrom);
            DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightTo);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.nextLesseeId);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getLessorId() {
        return lessorId;
    }

    public long getCurrentLesseeId() {
        return currentLesseeId;
    }

    public int getCurrentLeasingHeightFrom() {
        return currentLeasingHeightFrom;
    }

    public int getCurrentLeasingHeightTo() {
        return currentLeasingHeightTo;
    }

    public long getNextLesseeId() {
        return nextLesseeId;
    }

    public int getNextLeasingHeightFrom() {
        return nextLeasingHeightFrom;
    }

    public int getNextLeasingHeightTo() {
        return nextLeasingHeightTo;
    }
    
}
