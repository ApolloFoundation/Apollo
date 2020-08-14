/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public class AccountGuaranteedBalanceTable extends DerivedDbTable<AccountGuaranteedBalance> {

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
    public AccountGuaranteedBalanceTable(BlockchainConfig blockchainConfig,
                                         PropertiesHolder propertiesHolder,
                                         DerivedTablesRegistry derivedDbTablesRegistry,
                                         DatabaseManager databaseManager) {
        super(TABLE_NAME, derivedDbTablesRegistry, databaseManager, null);
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

        final int size = lessors.size();
        Long[] lessorIds = lessors.toArray(new Long[]{});
        String lessorParams;
        if (size == 1) {
            lessorParams = " = ?";
        } else if (size == 0) {
            return lessorsAdditions;
        } else {
            lessorParams = IntStream.range(0, lessors.size())
                .mapToObj(i -> "?")
                .collect(Collectors.joining(",", "IN (", ")"));
        }

        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT account_id, SUM (additions) AS " + ADDITIONS_COLUMN_NAME + " "
                 + "FROM account_guaranteed_balance T WHERE account_id " + lessorParams + " AND height > ? "
                 + (height < blockchainHeight ? " AND height <= ? " : "")
                 + " GROUP BY account_id ORDER BY account_id")
        ) {
            int i = 0;
            for (Object param : lessorIds) {
                pstmt.setObject(++i, param);
            }
            pstmt.setInt(++i, height - blockchainConfig.getGuaranteedBalanceConfirmations());
            if (height < blockchainHeight) {
                pstmt.setInt(++i, height);
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
             PreparedStatement pstmtUpdate = con.prepareStatement("INSERT INTO account_guaranteed_balance (account_id, "
                 + " additions, height) VALUES(?, ?, ?) "
                 + "ON DUPLICATE KEY UPDATE "
                 + "account_id = VALUES(account_id), additions = VALUES(additions), height = VALUES(height)")
        ) {
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
