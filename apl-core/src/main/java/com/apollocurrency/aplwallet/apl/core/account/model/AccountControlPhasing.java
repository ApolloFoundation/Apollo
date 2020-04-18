/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDeletableEntity;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Setter
@Getter
public class AccountControlPhasing extends VersionedDeletableEntity {

//    private final DbKey dbKey;
    private final long accountId;
    private long maxFees;
    private short minDuration;
    private short maxDuration;
    private PhasingParams phasingParams;

    public AccountControlPhasing(DbKey dbKey, long accountId, PhasingParams params, long maxFees, short minDuration, short maxDuration, int height) {
        super(null, height);
        this.accountId = accountId;
        setDbKey(dbKey);
        this.phasingParams = params;
        this.maxFees = maxFees;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    public AccountControlPhasing(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        setDbKey(dbKey);
        this.accountId = rs.getLong("account_id");
        Long[] whitelist = DbUtils.getArray(rs, "whitelist", Long[].class);
        phasingParams = new PhasingParams(rs.getByte("voting_model"),
            rs.getLong("holding_id"),
            rs.getLong("quorum"),
            rs.getLong("min_balance"),
            rs.getByte("min_balance_model"),
            whitelist == null ? Convert.EMPTY_LONG : Convert.toArray(whitelist));
        this.maxFees = rs.getLong("max_fees");
        this.minDuration = rs.getShort("min_duration");
        this.maxDuration = rs.getShort("max_duration");
    }
}
