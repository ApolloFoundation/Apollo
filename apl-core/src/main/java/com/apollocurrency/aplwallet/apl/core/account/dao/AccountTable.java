/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxValue;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.core.app.CollectionUtil.toList;

/**
 * @author al
 */
@Singleton
@Slf4j
public class AccountTable extends VersionedDeletableEntityDbTable<Account> {
    private static final LongKeyFactory<Account> accountDbKeyFactory = new LongKeyFactory<Account>("id") {
        @Override
        public DbKey newKey(Account account) {
            if (account.getDbKey() == null) {
                account.setDbKey(super.newKey(account.getId()));
            }
            return account.getDbKey();
        }
    };
    private final BlockchainConfig blockchainConfig;

    @Inject
    //TODO Remove references to the Blockchain and BlockchainConfig classes when the EntityDbTable class will be refactored
    public AccountTable(Blockchain blockchain, BlockchainConfig blockchainConfig/*, @Named("CREATOR_ID")long creatorId*/) {
        super("account", accountDbKeyFactory, false);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL.");
    }

    public static DbKey newKey(long id) {
        return accountDbKeyFactory.newKey(id);
    }

    public static DbKey newKey(Account a) {
        return accountDbKeyFactory.newKey(a);
    }

    @Override
    public Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Account(rs, dbKey);
    }

    @Override
    public void save(Connection con, Account account) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE) final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, "
                + "balance, unconfirmed_balance, forged_balance, "
                + "active_lessee_id, has_control_phasing, height, latest, deleted) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, account.getId());
            pstmt.setLong(++i, account.getBalanceATM());
            pstmt.setLong(++i, account.getUnconfirmedBalanceATM());
            pstmt.setLong(++i, account.getForgedBalanceATM());
            DbUtils.setLongZeroToNull(pstmt, ++i, account.getActiveLesseeId());
            pstmt.setBoolean(++i, account.getControls().contains(AccountControlType.PHASING_ONLY));
            pstmt.setInt(++i, account.getHeight());
            pstmt.executeUpdate();
        }
    }

    public List<Account> selectAllForKey(Long id) throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * from account where id = ? order by db_id DESC")) {
            pstmt.setLong(1, id);
            return CollectionUtil.toList(getManyBy(con, pstmt, false));
        }
    }


    @Override
    public void trim(int height) {
        if (height <= blockchainConfig.getGuaranteedBalanceConfirmations()) {
            return;
        }
        super.trim(height);
    }

    public long getTotalSupply(long creatorId) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT ABS(balance) AS total_supply FROM account WHERE id = ?")
        ) {
            int i = 0;
            pstmt.setLong(++i, creatorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_supply");
                } else {
                    throw new RuntimeException("Cannot retrieve total_supply: no data");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<Account> getTopHolders(int numberOfTopAccounts) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM account WHERE balance > 0 AND latest = true " +
                 " ORDER BY balance desc " + DbUtils.limitsClause(0, numberOfTopAccounts - 1))
        ) {
            int i = 0;
            DbUtils.setLimits(++i, pstmt, 0, numberOfTopAccounts - 1);
            return toList(getManyBy(con, pstmt, false));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public long getTotalAmountOnTopAccounts(int numberOfTopAccounts) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.NAMED_SUB_SELECT)
             PreparedStatement pstmt =
                 con.prepareStatement("SELECT sum(balance) as total_amount FROM (select balance from account WHERE balance > 0 AND latest = true" +
                     " ORDER BY balance desc " + DbUtils.limitsClause(0, numberOfTopAccounts - 1) + ")")) {
            int i = 0;
            DbUtils.setLimits(++i, pstmt, 0, numberOfTopAccounts - 1);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_amount");
                } else {
                    throw new RuntimeException("Cannot retrieve total_amount: no data");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public long getTotalNumberOfAccounts() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS number_of_accounts FROM account WHERE balance > 0 AND latest = true ")
        ) {
            if (rs.next()) {
                return rs.getLong("number_of_accounts");
            } else {
                throw new RuntimeException("Cannot retrieve number of accounts: no data");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public MinMaxValue getMinMaxValue(int height) {
        return super.getMinMaxValue(height, "id");
    }

}
