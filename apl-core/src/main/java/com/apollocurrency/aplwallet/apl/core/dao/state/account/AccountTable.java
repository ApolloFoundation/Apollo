/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;

/**
 * Initialization is done inside the {@link AccountTableCacheConfiguration}
 * @author al
 */
@Slf4j
public class AccountTable extends VersionedDeletableEntityDbTable<Account> implements AccountTableInterface {

    public AccountTable(DatabaseManager databaseManager,
                        Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("account", accountDbKeyFactory, null,
                databaseManager, deleteOnTrimDataEvent);
    }

    @Override
    public Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Account(rs, dbKey);
    }

    @Override
    public void save(Connection con, Account account) throws SQLException {
//        Optional<Account> existingOptional = selectLastExisting(con, account.getId());
//        if (existingOptional.isEmpty()) {
//            doInsert(con, account);
//            return;
//        }
//        Account existing = existingOptional.get();
//        if (existing.getHeight() != account.getHeight()) {
//            doInsert(con, account);
//            return;
//        }
//        doUpdate(con, account);
        if (account.requireMerge()) {
            doUpdate(con, account);
        } else {
            doInsert(con, account);
        }
    }

    private void doUpdate(Connection con, Account account) throws SQLException {
        try (
            final PreparedStatement pstmt = con.prepareStatement("UPDATE account SET "
                + "parent = ?, is_multi_sig = ?, addr_scope = ?, "
                + "balance = ?, unconfirmed_balance = ?, forged_balance = ?, "
                + "active_lessee_id = ?, has_control_phasing = ?, height = ?, latest = true, deleted = false WHERE db_id = ?"
            );
        ) {
            int i = 0;
            DbUtils.setLongZeroToNull(pstmt, ++i, account.getParentId());
            pstmt.setBoolean(++i, account.isMultiSig());
            pstmt.setByte(++i, account.getAddrScope().getCode());
            pstmt.setLong(++i, account.getBalanceATM());
            pstmt.setLong(++i, account.getUnconfirmedBalanceATM());
            pstmt.setLong(++i, account.getForgedBalanceATM());
            DbUtils.setLongZeroToNull(pstmt, ++i, account.getActiveLesseeId());
            pstmt.setBoolean(++i, account.getControls().contains(AccountControlType.PHASING_ONLY));
            pstmt.setInt(++i, account.getHeight());
            pstmt.setLong(++i, account.getDbId());
            pstmt.executeUpdate();
        }
    }

    private void doInsert(Connection con, Account account) throws SQLException {
        try (
            final PreparedStatement pstmt = con.prepareStatement("INSERT INTO account (id, "
                + "parent, is_multi_sig, addr_scope, "
                + "balance, unconfirmed_balance, forged_balance, "
                + "active_lessee_id, has_control_phasing, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) ", Statement.RETURN_GENERATED_KEYS
            )
        ) {
            int i = 0;
            pstmt.setLong(++i, account.getId());
            DbUtils.setLongZeroToNull(pstmt, ++i, account.getParentId());
            pstmt.setBoolean(++i, account.isMultiSig());
            pstmt.setByte(++i, account.getAddrScope().getCode());
            pstmt.setLong(++i, account.getBalanceATM());
            pstmt.setLong(++i, account.getUnconfirmedBalanceATM());
            pstmt.setLong(++i, account.getForgedBalanceATM());
            DbUtils.setLongZeroToNull(pstmt, ++i, account.getActiveLesseeId());
            pstmt.setBoolean(++i, account.getControls().contains(AccountControlType.PHASING_ONLY));
            pstmt.setInt(++i, account.getHeight());
            pstmt.executeUpdate();
            try (ResultSet dbIdRs = pstmt.getGeneratedKeys()) {
                if (!dbIdRs.next()) {
                    throw new SQLException("Unable to retrieve generated id for the account " + account);
                }
                account.setDbId(dbIdRs.getLong(1));
            }
        }

    }

    @Override
    public List<Account> selectAllForKey(Long id) throws SQLException {
        try (Connection con = getDatabaseManager().getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * from account where id = ? order by db_id DESC")) {
            pstmt.setLong(1, id);
            return CollectionUtil.toList(getManyBy(con, pstmt, false));
        }
    }

    @Override
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

    @Override
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

    @Override
    public long getTotalAmountOnTopAccounts(int numberOfTopAccounts) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.NAMED_SUB_SELECT)
             PreparedStatement pstmt =
                 con.prepareStatement("SELECT sum(balance) as total_amount FROM (select balance from account WHERE balance > 0 AND latest = true" +
                     " ORDER BY balance desc " + DbUtils.limitsClause(0, numberOfTopAccounts - 1) + ") as acc_ballance")) {
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

    @Override
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
    public List<Account> getRecentAccounts(int limit) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement recentPstm = con.prepareStatement("SELECT * from account INNER JOIN " +
                 "(SELECT id, max(db_id) as max_db_id FROM account WHERE latest=true GROUP BY id ORDER BY height DESC LIMIT " + limit + ") as recent_ids " +
                 "ON account.db_id = recent_ids.max_db_id")
        ) {
            return CollectionUtil.toList(getManyBy(con, recentPstm, false));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage() + ", limit " + limit, e);
        }

    }

    private Optional<Account> selectLastExisting(Connection connection, long id) throws SQLException {
        try (PreparedStatement pstm = connection.prepareStatement("SELECT * FROM " + table + " WHERE id = ? ORDER BY db_id DESC LIMIT 1")) {
            pstm.setLong(1, id);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(load(connection, rs, accountDbKeyFactory.newKey(id)));
                } else {
                    return Optional.empty();
                }
            }
        }
    }
}
