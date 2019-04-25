/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public final class PublicKey extends VersionedDerivedEntity {
    public final long accountId;
    byte[] publicKey;

    public PublicKey(long accountId, byte[] publicKey, int height) {
        super(null, height);
        this.accountId = accountId;
        this.publicKey = publicKey;
    }

    public PublicKey(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.accountId = rs.getLong("account_id");
        this.publicKey = rs.getBytes("public_key");
        setDbKey(dbKey);
    }

    public long getAccountId() {
        return accountId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }


}
