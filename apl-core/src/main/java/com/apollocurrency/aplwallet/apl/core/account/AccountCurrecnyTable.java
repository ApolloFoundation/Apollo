/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
class AccountCurrecnyTable extends VersionedEntityDbTable<AccountCurrency> {
    
    public AccountCurrecnyTable(String table, DbKey.Factory<AccountCurrency> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    @Override
    protected AccountCurrency load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountCurrency(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountCurrency accountCurrency) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_currency " + "(account_id, currency_id, units, unconfirmed_units, height, latest) " + "KEY (account_id, currency_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountCurrency.accountId);
            pstmt.setLong(++i, accountCurrency.currencyId);
            pstmt.setLong(++i, accountCurrency.units);
            pstmt.setLong(++i, accountCurrency.unconfirmedUnits);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    protected String defaultSort() {
        return " ORDER BY units DESC, account_id, currency_id ";
    }
    
}
