/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountGuaranteedBalanceTable extends DerivedDbTable {
    private static final String TABLE_NAME = "account_guaranteed_balance";
    private BlockchainConfig blockchainConfig;
    private int batchCommitSize;

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
        super(TABLE_NAME, false);
        this.blockchainConfig = blockchainConfig;
        this.batchCommitSize = propertiesHolder.BATCH_COMMIT_SIZE();
    }
}
