/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetTransfer;

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

    public DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ?"
                + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? ORDER BY height DESC, db_id DESC"
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ? AND asset_id = ?"
                + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? AND asset_id = ? ORDER BY height DESC, db_id DESC"
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, assetId);
            DbUtils.setLimits(++i, pstmt, from, to);
            return getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }


}
