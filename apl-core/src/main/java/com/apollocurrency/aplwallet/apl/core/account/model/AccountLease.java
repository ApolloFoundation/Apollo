/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
@ToString(callSuper = true)
@Getter @Setter
public final class AccountLease extends VersionedDerivedEntity {

    private final long lessorId;

    private long currentLesseeId;
    private int currentLeasingHeightFrom;
    private int currentLeasingHeightTo;
    private long nextLesseeId;
    private int nextLeasingHeightFrom;
    private int nextLeasingHeightTo;

    public AccountLease(long lessorId, int currentLeasingHeightFrom, int currentLeasingHeightTo, long currentLesseeId, int height) {
        super(null, height);
        this.lessorId = lessorId;
        this.currentLeasingHeightFrom = currentLeasingHeightFrom;
        this.currentLeasingHeightTo = currentLeasingHeightTo;
        this.currentLesseeId = currentLesseeId;
    }

    public AccountLease(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.lessorId = rs.getLong("lessor_id");
        this.currentLeasingHeightFrom = rs.getInt("current_leasing_height_from");
        this.currentLeasingHeightTo = rs.getInt("current_leasing_height_to");
        this.currentLesseeId = rs.getLong("current_lessee_id");
        this.nextLeasingHeightFrom = rs.getInt("next_leasing_height_from");
        this.nextLeasingHeightTo = rs.getInt("next_leasing_height_to");
        this.nextLesseeId = rs.getLong("next_lessee_id");
        setDbKey(dbKey);
    }

}
