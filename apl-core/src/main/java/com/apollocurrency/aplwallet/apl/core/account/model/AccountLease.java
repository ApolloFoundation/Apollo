/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.account.AccountLeaseTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */

@Getter @Setter
public final class AccountLease {
    //TODO remove the unneeded public scope
    public final long lessorId;
    public final DbKey dbKey;
    public long currentLesseeId;
    public int currentLeasingHeightFrom;
    public int currentLeasingHeightTo;
    public long nextLesseeId;
    public int nextLeasingHeightFrom;
    public int nextLeasingHeightTo;
    
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
    
}
