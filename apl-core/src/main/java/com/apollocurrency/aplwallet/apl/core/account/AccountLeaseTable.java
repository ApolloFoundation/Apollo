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
class AccountLeaseTable extends VersionedEntityDbTable<AccountLease> {
    
    public AccountLeaseTable(String table, DbKey.Factory<AccountLease> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    @Override
    protected AccountLease load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountLease(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountLease accountLease) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_lease " + "(lessor_id, current_leasing_height_from, current_leasing_height_to, current_lessee_id, " + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, height, latest) " + "KEY (lessor_id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountLease.lessorId);
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.currentLeasingHeightFrom);
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.currentLeasingHeightTo);
            DbUtils.setLongZeroToNull(pstmt, ++i, accountLease.currentLesseeId);
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.nextLeasingHeightFrom);
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.nextLeasingHeightTo);
            DbUtils.setLongZeroToNull(pstmt, ++i, accountLease.nextLesseeId);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }
    
}
