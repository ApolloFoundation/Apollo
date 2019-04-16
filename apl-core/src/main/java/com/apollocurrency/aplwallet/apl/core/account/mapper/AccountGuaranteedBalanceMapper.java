/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.mapper;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountGuaranteedBalance;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountGuaranteedBalanceMapper implements RowMapper<AccountGuaranteedBalance> {
    @Override
    public AccountGuaranteedBalance map(ResultSet rs, StatementContext ctx) throws SQLException {
        long accountId = rs.getLong("account_id");
        long additions = rs.getLong("additions");
        int height = rs.getInt("height");
        return new AccountGuaranteedBalance(accountId, additions, height);
    }
}
