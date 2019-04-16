/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.account.mapper.AccountGuaranteedBalanceMapper;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountGuaranteedBalanceTable extends EntityDbTable<AccountGuaranteedBalance> {
    private static final String TABLE_NAME = "account_guaranteed_balance";
    private static final KeyFactory<AccountGuaranteedBalance> KEY_FACTORY = new LongKeyFactory<>("account_id") {
        @Override
        public DbKey newKey(AccountGuaranteedBalance guaranteedBalance) {
            if (guaranteedBalance.getDbKey() == null) {
                guaranteedBalance.setDbKey(new LongKey(guaranteedBalance.getAccountId()));
            }
            return guaranteedBalance.getDbKey();
        }
    };
    private static final AccountGuaranteedBalanceMapper MAPPER = new AccountGuaranteedBalanceMapper();


    private BlockchainConfig blockchainConfig;
    private int batchCommitSize;

    @Override
    protected AccountGuaranteedBalance load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        AccountGuaranteedBalance guaranteedBalance = MAPPER.map(rs, null);
        guaranteedBalance.setDbKey(dbKey);
        return guaranteedBalance;
    }

    @Override
    protected void save(Connection con, AccountGuaranteedBalance guaranteedBalance) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement(
                "MERGE INTO account_guaranteed_balance (account_id, additions, height) KEY (account_id, height) VALUES(?, ?, ?)")) {
            pstmt.setLong(1, guaranteedBalance.getAccountId());
            pstmt.setLong(2, guaranteedBalance.getAdditions());
            pstmt.setInt(3, guaranteedBalance.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void trim(int height, TransactionalDataSource dataSource) {
        try (Connection con = dataSource.getConnection();
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

    @Inject
    public AccountGuaranteedBalanceTable(BlockchainConfig blockchainConfig, PropertiesHolder propertiesHolder) {
        super(TABLE_NAME, KEY_FACTORY, false);
        this.blockchainConfig = blockchainConfig;
        this.batchCommitSize = propertiesHolder.BATCH_COMMIT_SIZE();
    }
}
