/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

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
        try (
                final Connection con = dataSource.getConnection();
                @DatabaseSpecificDml(DmlMarker.DELETE_WITH_LIMIT_WITHOUT_LOCK)
                final PreparedStatement pstmtDelete = con.prepareStatement(
                        "DELETE FROM account_guaranteed_balance " +
                                "WHERE db_id IN " +
                                "(SELECT db_id FROM account_guaranteed_balance " +
                                "WHERE height < ? AND height >= 0 FETCH FIRST ? ROWS ONLY)"
                )
        ) {
            pstmtDelete.setInt(1, height - blockchainConfig.getGuaranteedBalanceConfirmations());
            pstmtDelete.setInt(2, batchCommitSize);
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

    @Inject
    public AccountGuaranteedBalanceTable(BlockchainConfig blockchainConfig, PropertiesHolder propertiesHolder) {
        super(TABLE_NAME, false);
        this.blockchainConfig = blockchainConfig;
        this.batchCommitSize = propertiesHolder.BATCH_COMMIT_SIZE();
    }
}
