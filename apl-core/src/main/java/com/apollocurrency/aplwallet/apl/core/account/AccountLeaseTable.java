/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public class AccountLeaseTable extends VersionedEntityDbTable<AccountLease> {
    private static final LongKeyFactory<AccountLease> accountLeaseDbKeyFactory = new LongKeyFactory<AccountLease>("lessor_id") {

        @Override
        public DbKey newKey(AccountLease accountLease) {
            return accountLease.dbKey;
        }

    };
    private static final AccountLeaseTable accountLeaseTable = new AccountLeaseTable();
  
    public static DbKey newKey(long id){
        return accountLeaseDbKeyFactory.newKey(id);
    } 
    public static AccountLeaseTable getInstance(){
        return accountLeaseTable;
    }
    public AccountLeaseTable() {
        super("account_lease", accountLeaseDbKeyFactory);
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
   

    public static int getAccountLeaseCount() {
        return accountLeaseTable.getCount();
    }
    static DbIterator<AccountLease> getLeaseChangingAccounts(final int height) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT * FROM account_lease WHERE current_leasing_height_from = ? AND latest = TRUE "
                            + "UNION ALL SELECT * FROM account_lease WHERE current_leasing_height_to = ? AND latest = TRUE "
                            + "ORDER BY current_lessee_id, lessor_id");
            int i = 0;
            pstmt.setInt(++i, height);
            pstmt.setInt(++i, height);
            return accountLeaseTable.getManyBy(con, pstmt, true);
        }
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }    
}
