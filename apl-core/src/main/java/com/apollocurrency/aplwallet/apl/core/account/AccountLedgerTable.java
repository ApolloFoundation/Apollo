/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
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
        try (final Connection con = db.getConnection()) {
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
    public void trim(int height) {
        if (AccountLedger.trimKeep <= 0) {
            return;
        }
        try (final Connection con = db.getConnection();
            final PreparedStatement pstmt = con.prepareStatement("DELETE FROM account_ledger WHERE height <= ? LIMIT " + AccountLedger.propertiesHolder.BATCH_COMMIT_SIZE())) {
            pstmt.setInt(1, Math.max(blockchain.getHeight() - AccountLedger.trimKeep, 0));
            int trimmed;
            do {
                trimmed = pstmt.executeUpdate();
                Db.getDb().commitTransaction();
            } while (trimmed >= AccountLedger.propertiesHolder.BATCH_COMMIT_SIZE());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
}
