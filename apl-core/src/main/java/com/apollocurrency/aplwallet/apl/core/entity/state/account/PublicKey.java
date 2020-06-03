/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.entity.state.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author al
 */
@Getter
@Setter
public final class PublicKey extends VersionedDerivedEntity {

    private final long accountId;
    private byte[] publicKey;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKey)) return false;
        if (!super.equals(o)) return false;
        PublicKey publicKey1 = (PublicKey) o;
        return accountId == publicKey1.accountId &&
            Arrays.equals(publicKey, publicKey1.publicKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), accountId);
        result = 31 * result + Arrays.hashCode(publicKey);
        return result;
    }

    @Override
    public String toString() {
        return "PublicKey{" +
            "accountId=" + accountId +
            ", publicKey=" + (publicKey != null ? "[...]" : "null") +
            ", height=" + getHeight() +
            ", latest=" + isLatest() +
            "} ";
    }
}
