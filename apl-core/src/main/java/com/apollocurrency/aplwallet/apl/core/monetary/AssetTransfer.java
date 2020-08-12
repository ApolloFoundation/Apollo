/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.monetary;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetTransfer;
import com.apollocurrency.aplwallet.apl.util.Listeners;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated
public final class AssetTransfer {

    private static final Listeners<AssetTransfer, Event> listeners = new Listeners<>();
    private static final LongKeyFactory<AssetTransfer> transferDbKeyFactory = new LongKeyFactory<AssetTransfer>("id") {

        @Override
        public DbKey newKey(AssetTransfer assetTransfer) {
            return assetTransfer.dbKey;
        }

    };
    private static final EntityDbTable<AssetTransfer> assetTransferTable = new EntityDbTable<AssetTransfer>("asset_transfer", transferDbKeyFactory) {

        @Override
        public AssetTransfer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new AssetTransfer(rs, dbKey);
        }

        @Override
        public void save(Connection con, AssetTransfer assetTransfer) throws SQLException {
            assetTransfer.save(con);
        }

    };
    private static DatabaseManager databaseManager;
    private final long id;
    private final DbKey dbKey;
    private final long assetId;
    private final int height;
    private final long senderId;
    private final long recipientId;
    private final long quantityATM;
    private final int timestamp;

    private AssetTransfer(Transaction transaction, ColoredCoinsAssetTransfer attachment) {
        this.id = transaction.getId();
        this.dbKey = transferDbKeyFactory.newKey(this.id);
        Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
        this.height = blockchain.getHeight();
        this.assetId = attachment.getAssetId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.quantityATM = attachment.getQuantityATU();
        this.timestamp = blockchain.getLastBlockTimestamp();
    }

    private AssetTransfer(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.assetId = rs.getLong("asset_id");
        this.senderId = rs.getLong("sender_id");
        this.recipientId = rs.getLong("recipient_id");
        this.quantityATM = rs.getLong("quantity");
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    /**
     * @deprecated
     */
    public static DbIterator<AssetTransfer> getAllTransfers(int from, int to) {
        return assetTransferTable.getAll(from, to);
    }

    /**
     * @deprecated
     */
    public static int getCount() {
        return assetTransferTable.getCount();
    }

/*
    public static boolean addListener(Listener<AssetTransfer> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<AssetTransfer> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static boolean addListener(Listener<AssetTransfer> listener) {
        return addListener(listener, Event.ASSET_TRANSFER);
    }

    public static boolean removeListener(Listener<AssetTransfer> listener) {
        return removeListener(listener, Event.ASSET_TRANSFER);
    }
*/

    /**
     * @deprecated
     */
    public static DbIterator<AssetTransfer> getAssetTransfers(long assetId, int from, int to) {
        return assetTransferTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
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
            return assetTransferTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * @deprecated
     */
    public static DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
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
            return assetTransferTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * @deprecated
     */
    public static int getTransferCount(long assetId) {
        return assetTransferTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

    /**
     * @deprecated
     */
    public static AssetTransfer addAssetTransfer(Transaction transaction, ColoredCoinsAssetTransfer attachment) {
        AssetTransfer assetTransfer = new AssetTransfer(transaction, attachment);
        assetTransferTable.insert(assetTransfer);
        listeners.notify(assetTransfer, Event.ASSET_TRANSFER);
        return assetTransfer;
    }

    /**
     * @deprecated
     */
    public static void init(DatabaseManager databaseManagerParam) {
        databaseManager = databaseManagerParam;
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_transfer (id, asset_id, "
            + "sender_id, recipient_id, quantity, `timestamp`, height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.senderId);
            pstmt.setLong(++i, this.recipientId);
            pstmt.setLong(++i, this.quantityATM);
            pstmt.setInt(++i, this.timestamp);
            pstmt.setInt(++i, this.height);
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public long getQuantityATU() {
        return quantityATM;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getHeight() {
        return height;
    }

    public enum Event {
        ASSET_TRANSFER
    }

}
