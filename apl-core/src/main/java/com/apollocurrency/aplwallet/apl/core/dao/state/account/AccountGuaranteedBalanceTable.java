/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class AccountGuaranteedBalanceTable extends DerivedDbTable {

    private static final String TABLE_NAME = "account_guaranteed_balance";

    private static final String ADDITIONS_COLUMN_NAME = "additions";
    private static final LongKeyFactory<AccountGuaranteedBalance>
        accountGuaranteedBalanceLongKeyFactory = new LongKeyFactory<>("account_id") {
        @Override
        public DbKey newKey(AccountGuaranteedBalance accountGuaranteedBalance) {
            if (accountGuaranteedBalance.getDbKey() == null) {
                accountGuaranteedBalance.setDbKey(super.newKey(accountGuaranteedBalance.getAccountId()));
            }
            return accountGuaranteedBalance.getDbKey();
        }
    };
    private final BlockchainConfig blockchainConfig;
    private final int batchCommitSize;

    @Inject
    public AccountGuaranteedBalanceTable(BlockchainConfig blockchainConfig, PropertiesHolder propertiesHolder) {
        super(TABLE_NAME, false);
        this.blockchainConfig = blockchainConfig;
        this.batchCommitSize = propertiesHolder.BATCH_COMMIT_SIZE();
    }

    public static DbKey newKey(long id) {
        return accountGuaranteedBalanceLongKeyFactory.newKey(id);
    }

    @Override
    public void trim(int height) {
        TransactionalDataSource dataSource = getDatabaseManager().getDataSource();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.DELETE_WITH_LIMIT)
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance "
                 + "WHERE height < ? AND height >= 0 LIMIT " + batchCommitSize)) {
            pstmtDelete.setInt(1, height - blockchainConfig.getGuaranteedBalanceConfirmations());
            int count;
            do {
                count = pstmtDelete.executeUpdate();
                dataSource.commit(false);
            } while (count >= batchCommitSize);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public AccountGuaranteedBalance load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountGuaranteedBalance(rs, dbKey);
    }

    public Long getSumOfAdditions(long accountId, int height, int currentHeight) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT SUM (" + ADDITIONS_COLUMN_NAME + ") AS " + ADDITIONS_COLUMN_NAME + " "
                 + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ? AND height <= ?")) {
            pstmt.setLong(1, accountId);
            pstmt.setInt(2, height);
            pstmt.setInt(3, currentHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getLong(ADDITIONS_COLUMN_NAME);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Map<Long, Long> getLessorsAdditions(List<Long> lessors, int height, int blockchainHeight) {
        Map<Long, Long> lessorsAdditions = new HashMap<>();
        Long[] lessorIds = lessors.toArray(new Long[]{});
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT account_id, SUM (additions) AS " + ADDITIONS_COLUMN_NAME + " "
                 + "FROM account_guaranteed_balance, TABLE (id BIGINT=?) T WHERE account_id = T.id AND height > ? "
                 + (height < blockchainHeight ? " AND height <= ? " : "")
                 + " GROUP BY account_id ORDER BY account_id")
        ) {
            pstmt.setObject(1, lessorIds);
            pstmt.setInt(2, height - blockchainConfig.getGuaranteedBalanceConfirmations());
            if (height < blockchainHeight) {
                pstmt.setInt(3, height);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long accountId = rs.getLong("account_id");
                    long sum = rs.getLong(ADDITIONS_COLUMN_NAME);
                    lessorsAdditions.put(accountId, sum);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return lessorsAdditions;
    }

    public void addToGuaranteedBalanceATM(long accountId, long amountATM, int blockchainHeight) {
        if (amountATM <= 0) {
            return;
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT additions FROM account_guaranteed_balance "
                 + "WHERE account_id = ? and height = ?");
             @DatabaseSpecificDml(DmlMarker.MERGE)
             PreparedStatement pstmtUpdate = con.prepareStatement("MERGE INTO account_guaranteed_balance (account_id, "
                 + " additions, height) KEY (account_id, height) VALUES(?, ?, ?)")) {
            pstmtSelect.setLong(1, accountId);
            pstmtSelect.setInt(2, blockchainHeight);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                long additions = amountATM;
                if (rs.next()) {
                    additions = Math.addExact(additions, rs.getLong(ADDITIONS_COLUMN_NAME));
                }
                pstmtUpdate.setLong(1, accountId);
                pstmtUpdate.setLong(2, additions);
                pstmtUpdate.setInt(3, blockchainHeight);
                pstmtUpdate.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
