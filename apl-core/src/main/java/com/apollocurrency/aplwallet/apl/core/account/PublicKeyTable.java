/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedPersistentDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 * @author al
 */
@Singleton
public class PublicKeyTable extends VersionedPersistentDbTable<PublicKey> {
    private static final PublicKeyDbFactory publicKeyDbKeyFactory = new PublicKeyDbFactory("account_id");

    private final Blockchain blockchain;

    private static class PublicKeyDbFactory extends LongKeyFactory<PublicKey> {

        public PublicKeyDbFactory(String idColumn) {
            super(idColumn);
        }

        @Override
        public DbKey newKey(PublicKey publicKey) {
            return publicKey.dbKey;
        }

        @Override
        public PublicKey newEntity(DbKey dbKey) {
            return new PublicKey(((LongKey) dbKey).getId(), null);
        }

    }
    
    public static DbKey newKey(long id){
        return publicKeyDbKeyFactory.newKey(id);
    }

    @Inject
    public PublicKeyTable(Blockchain blockchain) {
        super("public_key", publicKeyDbKeyFactory, true, null);
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain cannot be null");
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
