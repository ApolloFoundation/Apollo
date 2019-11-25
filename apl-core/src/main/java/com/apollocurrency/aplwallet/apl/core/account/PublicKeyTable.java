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
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 *
 * @author al
 */
@Singleton
public class PublicKeyTable extends EntityDbTable<PublicKey> {
    private static final PublicKeyDbFactory publicKeyDbKeyFactory = new PublicKeyDbFactory("account_id");

    private final Blockchain blockchain;

    private static class PublicKeyDbFactory extends LongKeyFactory<PublicKey> {

        public PublicKeyDbFactory(String idColumn) {
            super(idColumn);
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
            Blockchain blockchain = CDI.current().select(Blockchain.class).get();
            return new PublicKey(((LongKey) dbKey).getId(), null, blockchain.getHeight());
        }
    }

    public static DbKey newKey(long id){
        return publicKeyDbKeyFactory.newKey(id);
    }

    @Inject
    public PublicKeyTable(Blockchain blockchain) {
        super("public_key", publicKeyDbKeyFactory, true, null, false);
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
                final PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO " + table + " (account_id, public_key, height, latest) " +
                        "VALUES (?, ?, ?, TRUE) " +
                        "ON CONFLICT(account_id, height) " +
                        "DO UPDATE SET public_key = ?, latest = TRUE"
                )
        ) {
            int i = 0;
            pstmt.setLong(++i, publicKey.accountId);
            DbUtils.setBytes(pstmt, ++i, publicKey.publicKey);
            pstmt.setInt(++i, publicKey.getHeight());

            DbUtils.setBytes(pstmt, ++i, publicKey.publicKey);

            pstmt.executeUpdate();
        }
    }
}
