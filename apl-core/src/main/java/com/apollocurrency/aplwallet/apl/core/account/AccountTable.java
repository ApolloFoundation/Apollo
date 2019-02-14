/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
class AccountTable extends VersionedEntityDbTable<Account> {
    
    public AccountTable(String table, DbKey.Factory<Account> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    @Override
    protected Account load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Account(rs, dbKey);
    }

    @Override
    protected void save(Connection con, Account account) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, " + "balance, unconfirmed_balance, forged_balance, " + "active_lessee_id, has_control_phasing, height, latest) " + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, account.id);
            pstmt.setLong(++i, account.balanceATM);
            pstmt.setLong(++i, account.unconfirmedBalanceATM);
            pstmt.setLong(++i, account.forgedBalanceATM);
            DbUtils.setLongZeroToNull(pstmt, ++i, account.activeLesseeId);
            pstmt.setBoolean(++i, account.controls.contains(Account.ControlType.PHASING_ONLY));
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void trim(int height) {
        if (height <= Account.blockchainConfig.getGuaranteedBalanceConfirmations()) {
            return;
        }
        super.trim(height);
    }

    @Override
    public void checkAvailable(int height) {
        if (height > Account.blockchainConfig.getGuaranteedBalanceConfirmations()) {
            super.checkAvailable(height);
            return;
        }
        if (height > Account.blockchain.getHeight()) {
            throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Account.blockchain.getHeight());
        }
    }
    
}
