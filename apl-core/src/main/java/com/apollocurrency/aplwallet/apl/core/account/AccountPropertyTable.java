/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
class AccountPropertyTable extends VersionedEntityDbTable<AccountProperty> {
    
    public AccountPropertyTable(String table, DbKey.Factory<AccountProperty> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    @Override
    protected AccountProperty load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountProperty(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountProperty accountProperty) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_property " + "(id, recipient_id, setter_id, property, value, height, latest) " + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountProperty.id);
            pstmt.setLong(++i, accountProperty.recipientId);
            DbUtils.setLongZeroToNull(pstmt, ++i, accountProperty.setterId != accountProperty.recipientId ? accountProperty.setterId : 0);
            DbUtils.setString(pstmt, ++i, accountProperty.property);
            DbUtils.setString(pstmt, ++i, accountProperty.value);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }
    
}
