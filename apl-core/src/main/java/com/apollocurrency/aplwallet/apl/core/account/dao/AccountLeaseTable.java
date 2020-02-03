/*
 * Copyright © 2018-2019 Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountLease;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;

/**
 *
 * @author al
 */
@Slf4j
@Singleton
public class AccountLeaseTable extends VersionedDeletableEntityDbTable<AccountLease> {
    private static final LongKeyFactory<AccountLease> accountLeaseDbKeyFactory = new LongKeyFactory<AccountLease>("lessor_id") {

        @Override
        public DbKey newKey(AccountLease accountLease) {
            return accountLease.getDbKey() == null ? newKey(accountLease.getLessorId()) : accountLease.getDbKey();
        }

    };

    public static DbKey newKey(long id){
        return accountLeaseDbKeyFactory.newKey(id);
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
        if (log.isTraceEnabled()){
            log.trace("--lease-- Save accountLease={}", accountLease);
        }
        try (
                @DatabaseSpecificDml(DmlMarker.MERGE)
                final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_lease " +
                    "(lessor_id, current_leasing_height_from, current_leasing_height_to, current_lessee_id, " +
                    "next_leasing_height_from, next_leasing_height_to, next_lessee_id, height, latest) " +
                    "KEY (lessor_id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, accountLease.getLessorId());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getCurrentLeasingHeightFrom());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getCurrentLeasingHeightTo());
            DbUtils.setLongZeroToNull(pstmt, ++i, accountLease.getCurrentLesseeId());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getNextLeasingHeightFrom());
            DbUtils.setIntZeroToNull(pstmt, ++i, accountLease.getNextLeasingHeightTo());
            DbUtils.setLongZeroToNull(pstmt, ++i, accountLease.getNextLesseeId());
            pstmt.setInt(++i, accountLease.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public int rollback(int height) {
        //It's crucial to clear the active_lease_id field in the account table during the rolling back account_lease table;
        //Otherwise, rollback/popoff operation might lead to 'Generation signature verification failed' exception.
        int rc;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection();
            //TODO: Need to analyze, what statement is more suitable/effective "UPDATE account ac SET AC.ACTIVE_LESSEE_ID = null WHERE AC.ACTIVE_LESSEE_ID IS NOT NULL";
            PreparedStatement updateStmtClear = con.prepareStatement("UPDATE ACCOUNT ac set ac.ACTIVE_LESSEE_ID = null where ac.LATEST = true and ac.ID in (select lessor_id from ACCOUNT_LEASE AL where AL.CURRENT_LEASING_HEIGHT_FROM > ?  AND AL.LATEST=true)");
            PreparedStatement updateStmtSet = con.prepareStatement("UPDATE ACCOUNT ac set ac.ACTIVE_LESSEE_ID = ? where ac.ID = ? AND ac.LATEST = TRUE ");
        ) {
            int i = 0;
            updateStmtClear.setInt(++i, height);
            rc=updateStmtClear.executeUpdate();
            log.trace("--lease-- rollback updated Lease count={}, height={}", rc, height);
            rc=super.rollback(height);
            log.trace("--lease-- rollback (super.rollback) removed Lease count={}, height={}", rc, height);
            if (rc > 0){
                List<AccountLease> accountLeaseList = getLeaseChangingAccountsByInterval(height);
                if (log.isTraceEnabled()){
                    log.trace("--lease-- rollback found {} changed accounts", accountLeaseList.size());
                }
                for(AccountLease lease: accountLeaseList){
                    if (log.isTraceEnabled()){
                        log.trace("--lease-- rollback Update account id={} set activeLeaseId={}",
                            lease.getLessorId(), lease.getCurrentLesseeId());
                    }
                    i = 0;
                    updateStmtSet.setLong(++i, lease.getCurrentLesseeId());
                    updateStmtSet.setLong(++i, lease.getLessorId());
                    updateStmtSet.executeUpdate();
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return rc;
    }

    public int getAccountLeaseCount() {
        return getCount();
    }

    public List<AccountLease> getLeaseChangingAccountsOnExactlyHeight(final int height) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT * FROM account_lease WHERE current_leasing_height_from = ? AND latest = TRUE "
                            + "UNION ALL SELECT * FROM account_lease WHERE current_leasing_height_to = ? AND latest = TRUE "
                            + "ORDER BY current_lessee_id, lessor_id");
        ) {
            int i = 0;
            pstmt.setInt(++i, height);
            pstmt.setInt(++i, height);
            List<AccountLease> resultList = toList(getManyBy(con, pstmt, true));
            return resultList;
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public List<AccountLease> getLeaseChangingAccountsByInterval(final int height) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try(Connection con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                "SELECT * FROM account_lease WHERE current_leasing_height_from <= ? AND current_leasing_height_to >= ? AND latest = TRUE "
                    + "ORDER BY current_lessee_id, lessor_id");
        ) {
            int i = 0;
            pstmt.setInt(++i, height);
            pstmt.setInt(++i, height);
            List<AccountLease> resultList = toList(getManyBy(con, pstmt, true));
            return resultList;
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
