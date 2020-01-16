/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountGuaranteedBalanceTable extends DerivedDbTable {
    private static final String TABLE_NAME = "account_guaranteed_balance";
    private BlockchainConfig blockchainConfig;
    private int batchCommitSize;

    private static final LongKeyFactory<AccountGuaranteedBalance>
            accountGuaranteedBalanceLongKeyFactory = new LongKeyFactory<>("account_id") {
        @Override
        public DbKey newKey(AccountGuaranteedBalance accountGuaranteedBalance) {
            return accountGuaranteedBalance.dbKey;
        }
    };

    public static DbKey newKey(long id){
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
        }
        catch (SQLException e) {
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
             PreparedStatement pstmt = con.prepareStatement("SELECT SUM (additions) AS additions "
                 + "FROM account_guaranteed_balance WHERE account_id = ? AND height > ? AND height <= ?")) {
            pstmt.setLong(1, accountId);
            pstmt.setInt(2, height);
            pstmt.setInt(3, currentHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getLong("additions");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
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
                    additions = Math.addExact(additions, rs.getLong("additions"));
                }
                pstmtUpdate.setLong(1, accountId);
                pstmtUpdate.setLong(2, additions);
                pstmtUpdate.setInt(3, blockchainHeight);
                pstmtUpdate.executeUpdate();
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Inject
    public AccountGuaranteedBalanceTable(BlockchainConfig blockchainConfig, PropertiesHolder propertiesHolder) {
        super(TABLE_NAME, false);
        this.blockchainConfig = blockchainConfig;
        this.batchCommitSize = propertiesHolder.BATCH_COMMIT_SIZE();
    }
}
