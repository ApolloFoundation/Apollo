/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.operation.account;

import com.apollocurrency.aplwallet.apl.core.dao.operation.account.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Account Guaranty Balance Entity class
 */
@Setter
@Getter
@ToString(callSuper = true)
public class AccountGuaranteedBalance extends DerivedEntity {

    private final long accountId;
    private long additions;

    public AccountGuaranteedBalance(long accountId, long additions, int height) {
        super(null, height);
        this.accountId = accountId;
        this.additions = additions;
        setDbKey(AccountGuaranteedBalanceTable.newKey(this.accountId));
    }

    public AccountGuaranteedBalance(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.accountId = rs.getLong("account_id");
        this.additions = rs.getLong("additions");
        setDbKey(dbKey);
    }
}
