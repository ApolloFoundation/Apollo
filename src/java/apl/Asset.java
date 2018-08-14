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
 * Copyright © 2018 Apollo Foundation
 */

package apl;

import apl.db.DbClause;
import apl.db.DbIterator;
import apl.db.DbKey;
import apl.db.VersionedEntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Asset {

    private static final DbKey.LongKeyFactory<Asset> assetDbKeyFactory = new DbKey.LongKeyFactory<Asset>("id") {

        @Override
        public DbKey newKey(Asset asset) {
            return asset.dbKey;
        }

    };

    private static final VersionedEntityDbTable<Asset> assetTable = new VersionedEntityDbTable<Asset>("asset", assetDbKeyFactory, "name,description") {

        @Override
        protected Asset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Asset(rs, dbKey);
        }

        @Override
        protected void save(Connection con, Asset asset) throws SQLException {
            asset.save(con);
        }

        @Override
        public void trim(int height) {
            super.trim(Math.max(0, height - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK));
        }

        @Override
        public void checkAvailable(int height) {
            if (height + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK < Apl.getBlockchainProcessor().getMinRollbackHeight()) {
                throw new IllegalArgumentException("Historical data as of height " + height + " not available.");
            }
            if (height > Apl.getBlockchain().getHeight()) {
                throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Apl.getBlockchain().getHeight());
            }
        }

        @Override
        protected String defaultSort() {
            return super.defaultSort();
        }
    };

    public static DbIterator<Asset> getAllAssets(int from, int to) {
        return assetTable.getAll(from, to);
    }

    public static int getCount() {
        return assetTable.getCount();
    }

    public static Asset getAsset(long id) {
        return assetTable.get(assetDbKeyFactory.newKey(id));
    }

    public static Asset getAsset(long id, int height) {
        return assetTable.get(assetDbKeyFactory.newKey(id), height);
    }

    public static DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
        return assetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<Asset> searchAssets(String query, int from, int to) {
        return assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC ");
    }

    static void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
        assetTable.insert(new Asset(transaction, attachment));
    }

    static void deleteAsset(Transaction transaction, long assetId, long quantityATU) {
        Asset asset = getAsset(assetId);
        asset.quantityATU = Math.max(0, asset.quantityATU - quantityATU);
        assetTable.insert(asset);
        AssetDelete.addAssetDelete(transaction, assetId, quantityATU);
    }

    static void init() {
    }


    private final long assetId;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final long initialQuantityATU;
    private long quantityATU;
    private final byte decimals;

    private Asset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
        this.assetId = transaction.getId();
        this.dbKey = assetDbKeyFactory.newKey(this.assetId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityATU = attachment.getQuantityATU();
        this.initialQuantityATU = this.quantityATU;
        this.decimals = attachment.getDecimals();
    }

    private Asset(ResultSet rs, DbKey dbKey) throws SQLException {
        this.assetId = rs.getLong("id");
        this.dbKey = dbKey;
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.initialQuantityATU = rs.getLong("initial_quantity");
        this.quantityATU = rs.getLong("quantity");
        this.decimals = rs.getByte("decimals");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO asset "
                + "(id, account_id, name, description, initial_quantity, quantity, decimals, height, latest) "
                + "KEY(id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setLong(++i, this.initialQuantityATU);
            pstmt.setLong(++i, this.quantityATU);
            pstmt.setByte(++i, this.decimals);
            pstmt.setInt(++i, Apl.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return assetId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getInitialQuantityATU() {
        return initialQuantityATU;
    }

    public long getQuantityATU() {
        return quantityATU;
    }

    public byte getDecimals() {
        return decimals;
    }

    public DbIterator<Account.AccountAsset> getAccounts(int from, int to) {
        return Account.getAssetAccounts(this.assetId, from, to);
    }

    public DbIterator<Account.AccountAsset> getAccounts(int height, int from, int to) {
        return Account.getAssetAccounts(this.assetId, height, from, to);
    }

    public DbIterator<Trade> getTrades(int from, int to) {
        return Trade.getAssetTrades(this.assetId, from, to);
    }

    public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
        return AssetTransfer.getAssetTransfers(this.assetId, from, to);
    }

}
