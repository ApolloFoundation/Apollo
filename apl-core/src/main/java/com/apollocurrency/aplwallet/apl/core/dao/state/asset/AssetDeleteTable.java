/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDelete;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class AssetDeleteTable extends EntityDbTable<AssetDelete> {

    public static final LongKeyFactory<AssetDelete> deleteDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(AssetDelete assetDelete) {
            if (assetDelete.getDbKey() == null) {
                assetDelete.setDbKey(super.newKey(assetDelete.getId()));
            }
            return assetDelete.getDbKey();
        }
    };

    @Inject
    public AssetDeleteTable(DatabaseManager databaseManager,
                            Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("asset_delete", deleteDbKeyFactory, false, null,
                databaseManager, deleteOnTrimDataEvent);
    }

    @Override
    public AssetDelete load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AssetDelete(rs, dbKey);
    }

    @Override
    public void save(Connection con, AssetDelete assetDelete) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_delete (id, asset_id, "
            + "account_id, quantity, `timestamp`, height) "
            + "VALUES (?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, assetDelete.getId());
            pstmt.setLong(++i, assetDelete.getAssetId());
            pstmt.setLong(++i, assetDelete.getAccountId());
            pstmt.setLong(++i, assetDelete.getQuantityATU());
            pstmt.setInt(++i, assetDelete.getTimestamp());
            pstmt.setInt(++i, assetDelete.getHeight());
            pstmt.executeUpdate();
        }
    }

}
