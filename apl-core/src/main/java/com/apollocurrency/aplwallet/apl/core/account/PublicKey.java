/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.enterprise.inject.spi.CDI;

/**
 *
 * @author al
 */
public final class PublicKey {
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    public final long accountId;
    final DbKey dbKey;
    byte[] publicKey;
    int height;

    public PublicKey(long accountId, byte[] publicKey) {
        this.accountId = accountId;
        this.dbKey = PublicKeyTable.newKey(accountId);
        this.publicKey = publicKey;
        this.height = blockchain.getHeight();
    }

    public PublicKey(ResultSet rs, DbKey dbKey) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.dbKey = dbKey;
        this.publicKey = rs.getBytes("public_key");
        this.height = rs.getInt("height");
    }

    public long getAccountId() {
        return accountId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public int getHeight() {
        return height;
    }
    
}
