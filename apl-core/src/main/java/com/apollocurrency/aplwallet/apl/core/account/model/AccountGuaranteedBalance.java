/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity class
 */
@Setter @Getter
public class AccountGuaranteedBalance {

    private final long accountId;
    private final DbKey dbKey;
    private long additions;
    private int height;

    public AccountGuaranteedBalance(long accountId, long additions, int height) {
        this.accountId = accountId;
        this.additions = additions;
        this.dbKey = AccountGuaranteedBalanceTable.newKey(this.accountId);
        this.height = height;
    }

    public AccountGuaranteedBalance(ResultSet rs, DbKey dbKey) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.additions = rs.getLong("additions");
        this.height = rs.getInt("height");
        this.dbKey = dbKey;
    }

    @Override
    public String toString() {
        return "AccountGuaranteedBalance account_id: "
                + Long.toUnsignedString(accountId) + " additions: " + additions + " height: " + height;
    }
    
}
