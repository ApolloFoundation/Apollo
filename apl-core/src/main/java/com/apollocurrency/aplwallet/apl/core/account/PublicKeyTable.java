/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 *
 * @author al
 */
public class PublicKeyTable extends EntityDbTable<PublicKey> implements EntityDbTableInterface<PublicKey> {

    private final Blockchain blockchain;

    private static class PublicKeyDbFactory extends LongKeyFactory<PublicKey> {
        private final Blockchain blockchain;

        public PublicKeyDbFactory(String idColumn, Blockchain blockchain) {
            super(idColumn);
            this.blockchain = blockchain;
        }

        @Override
        public DbKey newKey(PublicKey publicKey) {
            if (publicKey.getDbKey() == null) {
                publicKey.setDbKey(new LongKey(publicKey.getAccountId()));
            }
            return publicKey.getDbKey();
        }

        @Override
        public PublicKey newEntity(DbKey dbKey) {
            return new PublicKey(((LongKey) dbKey).getId(), null, blockchain.getHeight());
        }
    }

    public PublicKeyTable(Blockchain blockchain) {
        super("public_key", new PublicKeyDbFactory("account_id", blockchain), true, null, true);
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain cannot be null");
    }

    @Override
    public PublicKey load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new PublicKey(rs, dbKey);
    }

    @Override
    public void save(Connection con, PublicKey publicKey) throws SQLException {
        publicKey.setHeight(blockchain.getHeight());
        try (
                @DatabaseSpecificDml(DmlMarker.MERGE)
                final PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (account_id, public_key, height, latest) " + "KEY (account_id, height) VALUES (?, ?, ?, TRUE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, publicKey.accountId);
            DbUtils.setBytes(pstmt, ++i, publicKey.publicKey);
            pstmt.setInt(++i, publicKey.getHeight());
            pstmt.executeUpdate();
        }
    }

}
