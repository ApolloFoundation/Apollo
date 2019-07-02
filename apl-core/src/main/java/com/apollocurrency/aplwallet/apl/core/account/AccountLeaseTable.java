/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainHelper;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public class AccountLeaseTable extends VersionedDeletableEntityDbTable<AccountLease> {
    private static final LongKeyFactory<AccountLease> accountLeaseDbKeyFactory = new LongKeyFactory<AccountLease>("lessor_id") {

        @Override
        public DbKey newKey(AccountLease accountLease) {
            return accountLease.getDbKey();
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
    public AccountLease load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountLease(rs, dbKey);
    }

    @Override
    public void save(Connection con, AccountLease accountLease) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_lease " + "(lessor_id, current_leasing_height_from, current_leasing_height_to, current_lessee_id, " + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, height, latest) " + "KEY (lessor_id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountLease.getLessorId());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getCurrentLeasingHeightFrom());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getCurrentLeasingHeightTo());
            DbUtils.setLongZeroToNull(pstmt, ++i, accountLease.getCurrentLesseeId());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getNextLeasingHeightFrom());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getNextLeasingHeightTo());
            DbUtils.setLongZeroToNull(pstmt, ++i, accountLease.getNextLesseeId());
            pstmt.setInt(++i, BlockchainHelper.getBlockchainHeight());
            pstmt.executeUpdate();
        }
    }
   

    public static int getAccountLeaseCount() {
        return accountLeaseTable.getCount();
    }

    public DbIterator<AccountLease> getLeaseChangingAccounts(final int height) {
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
