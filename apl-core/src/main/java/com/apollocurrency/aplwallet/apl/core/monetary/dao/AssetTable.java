/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.monetary.model.Asset;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

public class AssetTable extends VersionedDeletableEntityDbTable<Asset> {

    public static final LongKeyFactory<Asset> assetDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Asset asset) {
            if (asset.getDbKey() == null) {
                asset.setDbKey(super.newKey(asset.getId()));
            }
            return asset.getDbKey();
        }
    };

    public AssetTable() {
        super("asset", assetDbKeyFactory, "name, description");
    }

    @Override
    public Asset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Asset(rs, dbKey);
    }

    @Override
    public void save(Connection con, Asset asset) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO asset "
                + "(id, account_id, name, description, initial_quantity, quantity, decimals, height, latest, deleted) "
                + "KEY(id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, asset.getId());
            pstmt.setLong(++i, asset.getAccountId());
            pstmt.setString(++i, asset.getName());
            pstmt.setString(++i, asset.getDescription());
            pstmt.setLong(++i, asset.getInitialQuantityATU());
            pstmt.setLong(++i, asset.getQuantityATU());
            pstmt.setByte(++i, asset.getDecimals());
            pstmt.setInt(++i, asset.getHeight());
            pstmt.executeUpdate();
        }
    }


}
