/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
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
        this.dbKey = AccountLeaseTable.newKey(this.lessorId);
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
