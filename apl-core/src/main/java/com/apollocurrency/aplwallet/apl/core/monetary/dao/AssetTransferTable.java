/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary.dao;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.monetary.model.AssetTransfer;

@Singleton
public final class AssetTransferTable extends EntityDbTable<AssetTransfer> {

    public static final LongKeyFactory<AssetTransfer> assetTransferDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(AssetTransfer assetTransfer) {
            if (assetTransfer.getDbKey() == null) {
                assetTransfer.setDbKey(super.newKey(assetTransfer.getId()));
            }
            return assetTransfer.getDbKey();
        }

    };

    protected AssetTransferTable() {
        super("asset_transfer", assetTransferDbKeyFactory);
    }

    @Override
    public AssetTransfer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AssetTransfer(rs, dbKey);
    }

    @Override
    public void save(Connection con, AssetTransfer assetTransfer) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_transfer (id, asset_id, "
            + "sender_id, recipient_id, quantity, timestamp, height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, assetTransfer.getId());
            pstmt.setLong(++i, assetTransfer.getAssetId());
            pstmt.setLong(++i, assetTransfer.getSenderId());
            pstmt.setLong(++i, assetTransfer.getRecipientId());
            pstmt.setLong(++i, assetTransfer.getQuantityATM());
            pstmt.setInt(++i, assetTransfer.getTimestamp());
            pstmt.setInt(++i, assetTransfer.getHeight());
            pstmt.executeUpdate();
        }
    }

}
