/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.operation.publickey;

import com.apollocurrency.aplwallet.apl.core.entity.operation.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * @author al
 */
//@Singleton
public class GenesisPublicKeyTable extends EntityDbTable<PublicKey> {
    private final Blockchain blockchain;

    //@Inject
    public GenesisPublicKeyTable(Blockchain blockchain) {
        super("genesis_public_key", new PublicKeyDbFactory("account_id"), false, null, true);
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
            @DatabaseSpecificDml(DmlMarker.MERGE) final PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table
                + " (account_id, public_key, height, latest) "
                + "KEY (account_id, height) VALUES (?, ?, ?, TRUE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, publicKey.getAccountId());
            DbUtils.setBytes(pstmt, ++i, publicKey.getPublicKey());
            pstmt.setInt(++i, publicKey.getHeight());
            pstmt.executeUpdate();
        }
    }

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
    }

}
