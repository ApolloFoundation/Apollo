/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
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
    public class GenesisPublicKeyTable  extends VersionedPersistentDbTable<PublicKey> {
    
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

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
    private static final PublicKeyDbFactory publicKeyDbKeyFactory = new PublicKeyDbFactory("account_id");
    private static final GenesisPublicKeyTable publicKeyTable = new GenesisPublicKeyTable();
    
    public static  GenesisPublicKeyTable getInstance(){
        return publicKeyTable;
    }
    
    public static DbKey newKey(long id){
        return publicKeyDbKeyFactory.newKey(id);
    }
    
    protected GenesisPublicKeyTable() {
        super("genesis_public_key", publicKeyDbKeyFactory, false, null);
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
