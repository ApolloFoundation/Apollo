/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.VersionedPersistentDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

/**
 *
 * @author al
 */
class PublicKeyTable extends VersionedPersistentDbTable<PublicKey> {
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    
    protected PublicKeyTable(String table, DbKey.Factory<PublicKey> dbKeyFactory) {
        super(table, dbKeyFactory);
    }

    protected PublicKeyTable(String table, DbKey.Factory<PublicKey> dbKeyFactory, boolean multiversion) {
        super(table, dbKeyFactory, multiversion, null);
    }

    @Override
    protected PublicKey load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new PublicKey(rs, dbKey);
    }

    @Override
    protected void save(Connection con, PublicKey publicKey) throws SQLException {
        publicKey.height = blockchain.getHeight();
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (account_id, public_key, height, latest) " + "KEY (account_id, height) VALUES (?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, publicKey.accountId);
            DbUtils.setBytes(pstmt, ++i, publicKey.publicKey);
            pstmt.setInt(++i, publicKey.height);
            pstmt.executeUpdate();
        }
    }
    
}
