/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.EnumSet;

/**
 *
 * @author al
 */
public class AccountTable extends VersionedEntityDbTable<Account> {
    private static final LongKeyFactory<Account> accountDbKeyFactory = new LongKeyFactory<Account>("id") {

        @Override
        public DbKey newKey(Account account) {
            return account.dbKey == null ? newKey(account.id) : account.dbKey;
        }

        @Override
        public Account newEntity(DbKey dbKey) {
            return new Account(((LongKey) dbKey).getId());
        }

    };
    private static final AccountTable accountTable = new AccountTable();
    
    public static AccountTable getInstance(){
        return accountTable;
    }
    
    public static DbKey newKey(long id){
        return accountDbKeyFactory.newKey(id);
    }
    
    public static DbKey newKey(Account a){
        return accountDbKeyFactory.newKey(a);
    }
    
    public AccountTable() {
        super("account", accountDbKeyFactory);
    }

    @Override
    protected Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        long id = rs.getLong("id");
        Account res = new Account(id);
        res.dbKey = dbKey;
        res.balanceATM = rs.getLong("balance");
        res.unconfirmedBalanceATM = rs.getLong("unconfirmed_balance");
        res.forgedBalanceATM = rs.getLong("forged_balance");
        res.activeLesseeId = rs.getLong("active_lessee_id");
        if (rs.getBoolean("has_control_phasing")) {
            res.controls = Collections.unmodifiableSet(EnumSet.of(Account.ControlType.PHASING_ONLY));
        } else {
            res.controls = Collections.emptySet();
        }
        return res;
    }

    @Override
    protected void save(Connection con, Account account) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, " + "balance, unconfirmed_balance, forged_balance, " + "active_lessee_id, has_control_phasing, height, latest) " + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, account.id);
            pstmt.setLong(++i, account.balanceATM);
            pstmt.setLong(++i, account.unconfirmedBalanceATM);
            pstmt.setLong(++i, account.forgedBalanceATM);
            DbUtils.setLongZeroToNull(pstmt, ++i, account.activeLesseeId);
            pstmt.setBoolean(++i, account.controls.contains(Account.ControlType.PHASING_ONLY));
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void trim(int height, TransactionalDataSource dataSource) {
        if (height <= Account.blockchainConfig.getGuaranteedBalanceConfirmations()) {
            return;
        }
        super.trim(height, dataSource);
    }

    @Override
    public void checkAvailable(int height) {
        if (height > Account.blockchainConfig.getGuaranteedBalanceConfirmations()) {
            super.checkAvailable(height);
            return;
        }
        if (height > Account.blockchain.getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Account.blockchain.getHeight());
        }
    }
 
    public static long getTotalSupply(Connection con) throws SQLException {
        try (
                PreparedStatement pstmt =con.prepareStatement("SELECT ABS(balance) AS total_supply FROM account WHERE id = ?")
        ) {
            int i = 0;
            pstmt.setLong(++i, Genesis.CREATOR_ID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_supply");
                } else {
                    throw new RuntimeException("Cannot retrieve total_amount: no data");
                }
            }
        }
    }
     public static DbIterator<Account> getTopHolders(Connection con, int numberOfTopAccounts) throws SQLException {
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM account WHERE balance > 0 AND latest = true " +
                            " ORDER BY balance desc "+ DbUtils.limitsClause(0, numberOfTopAccounts - 1));
            int i = 0;
            DbUtils.setLimits(++i, pstmt, 0, numberOfTopAccounts - 1);
            return AccountTable.getInstance().getManyBy(con, pstmt, false);
    }
     
    public static long getTotalAmountOnTopAccounts(Connection con, int numberOfTopAccounts) throws SQLException {
        try (
                PreparedStatement pstmt =
                        con.prepareStatement("SELECT sum(balance) as total_amount FROM (select balance from account WHERE balance > 0 AND latest = true" +
                                " ORDER BY balance desc "+ DbUtils.limitsClause(0, numberOfTopAccounts - 1)+")") ) {
            int i = 0;
            DbUtils.setLimits(++i, pstmt, 0, numberOfTopAccounts - 1);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_amount");
                } else {
                    throw new RuntimeException("Cannot retrieve total_amount: no data");
                }
            }
        }
    }  

    public static long getTotalNumberOfAccounts(Connection con) throws SQLException {
        try (
                Statement stmt =con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS number_of_accounts FROM account WHERE balance > 0 AND latest = true ")
        ) {
            if (rs.next()) {
                return rs.getLong("number_of_accounts");
            } else {
                throw new RuntimeException("Cannot retrieve number of accounts: no data");
            }
        }
    }
   
}
