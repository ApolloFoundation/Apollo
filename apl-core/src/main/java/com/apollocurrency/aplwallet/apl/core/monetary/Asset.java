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

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAssetIssuance;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Deprecated
public final class Asset {

    private static final BlockChainInfoService BLOCK_CHAIN_INFO_SERVICE =
        CDI.current().select(BlockChainInfoService.class).get();

    /**
     * @deprecated
     */
    private static final LongKeyFactory<Asset> assetDbKeyFactory = new LongKeyFactory<Asset>("id") {

        @Override
        public DbKey newKey(Asset asset) {
            return asset.dbKey;
        }

    };

    /**
     * @deprecated
     */
    private static final VersionedDeletableEntityDbTable<Asset> assetTable
        = new VersionedDeletableEntityDbTable<Asset>("asset", assetDbKeyFactory, "name,description") {

        @Override
        public Asset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Asset(rs, dbKey);
        }

        @Override
        public void save(Connection con, Asset asset) throws SQLException {
            asset.save(con);
        }
    };
    private final long assetId;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final long initialQuantityATU;
    private final byte decimals;
    private long quantityATU;

    /**
     * @deprecated
     */
    private Asset(Transaction transaction, ColoredCoinsAssetIssuance attachment) {
        this.assetId = transaction.getId();
        this.dbKey = assetDbKeyFactory.newKey(this.assetId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityATU = attachment.getQuantityATU();
        this.initialQuantityATU = this.quantityATU;
        this.decimals = attachment.getDecimals();
    }

    /**
     * @deprecated
     */
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

    /**
     * @deprecated
     */
    public static DbIterator<Asset> getAllAssets(int from, int to) {
        return assetTable.getAll(from, to);
    }

    /**
     * @deprecated
     */
    public static int getCount() {
        return assetTable.getCount();
    }

    /**
     * @deprecated
     */
    public static Asset getAsset(long id) {
        return assetTable.get(assetDbKeyFactory.newKey(id));
    }

    /**
     * @deprecated
     */
    public static Asset getAsset(long id, int height) {
        final DbKey dbKey = assetDbKeyFactory.newKey(id);
        if (height < 0 || BLOCK_CHAIN_INFO_SERVICE.doesNotExceed(height)) {
            return assetTable.get(dbKey);
        }
        BLOCK_CHAIN_INFO_SERVICE.checkAvailable(height);

        return assetTable.get(dbKey, height);
    }

    /**
     * @deprecated
     */
    public static DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
        return assetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    /**
     * @deprecated
     */
    public static DbIterator<Asset> searchAssets(String query, int from, int to) {
        return assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC ");
    }

    /**
     * @deprecated
     */
    public static void addAsset(Transaction transaction, ColoredCoinsAssetIssuance attachment) {
        assetTable.insert(new Asset(transaction, attachment));
    }

    /**
     * @deprecated
     */
    public static void deleteAsset(Transaction transaction, long assetId, long quantityATU) {
        Asset asset = getAsset(assetId);
        asset.quantityATU = Math.max(0, asset.quantityATU - quantityATU);
        assetTable.insert(asset);
        AssetDelete.addAssetDelete(transaction, assetId, quantityATU);
    }

    /**
     * @deprecated
     */
    public static void init() {
    }

    private void save(Connection con) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset "
                + "(id, account_id, `name`, description, initial_quantity, quantity, decimals, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE "
                + "id = VALUES(id) , account_id = VALUES(account_id), `name` = VALUES(`name`), description = VALUES(description), "
                + "initial_quantity = VALUES(initial_quantity), quantity = VALUES(quantity), "
                + "decimals = VALUES(decimals), height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setLong(++i, this.initialQuantityATU);
            pstmt.setLong(++i, this.quantityATU);
            pstmt.setByte(++i, this.decimals);
            pstmt.setInt(++i, BLOCK_CHAIN_INFO_SERVICE.getHeight());
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
}
