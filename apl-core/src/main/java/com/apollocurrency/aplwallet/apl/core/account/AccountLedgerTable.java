/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import static com.apollocurrency.aplwallet.apl.core.account.AccountLedger.propertiesHolder;
import static com.apollocurrency.aplwallet.apl.core.account.AccountLedger.trimKeep;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

/**
 * Account ledger table
 */
public class AccountLedgerTable extends DerivedDbTable {
    private static final Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
        /**
         * Create the account ledger table
         */
        public AccountLedgerTable() {
            super("account_ledger");
        }

        /**
         * Insert an entry into the table
         *
         * @param   ledgerEntry             Ledger entry
         */
        public void insert(LedgerEntry ledgerEntry) {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try (Connection con = dataSource.getConnection()) {
                ledgerEntry.save(con);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        /**
         * Trim the account ledger table
         *
         * @param   height                  Trim height
         */
        @Override
        public void trim(int height, TransactionalDataSource dataSource) {
            if (trimKeep <= 0)
                return;
            if (dataSource == null) {
                dataSource = databaseManager.getDataSource();
            }
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("DELETE FROM account_ledger WHERE height <= ? LIMIT " + propertiesHolder.BATCH_COMMIT_SIZE())) {
                pstmt.setInt(1, Math.max(blockchain.getHeight() - trimKeep, 0));
                int trimmed;
                do {
                    trimmed = pstmt.executeUpdate();
                    dataSource.commit(false);
                } while (trimmed >= propertiesHolder.BATCH_COMMIT_SIZE());
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
    
}
