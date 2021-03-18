/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.publickey;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Uses external weld initialization through {@link PublicKeyTableProducer}
 * @author al
 */
public class GenesisPublicKeyTable extends EntityDbTable<PublicKey> {

    public GenesisPublicKeyTable(DatabaseManager databaseManager,
                                 Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("genesis_public_key", new PublicKeyDbFactory("account_id"), false, null,
            databaseManager, deleteOnTrimDataEvent);
    }

    @Override
    public PublicKey load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new PublicKey(rs, dbKey);
    }

    @Override
    public void save(Connection con, PublicKey publicKey) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE) final PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + table
                + " (account_id, public_key, height, latest) "
                + "VALUES (?, ?, ?, TRUE) "
                + "ON DUPLICATE KEY UPDATE account_id = VALUES(account_id), public_key = VALUES(public_key), "
                + "height = VALUES(height), latest = TRUE")
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
