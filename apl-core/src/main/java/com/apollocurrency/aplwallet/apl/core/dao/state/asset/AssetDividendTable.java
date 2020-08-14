/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;

@Singleton
public final class AssetDividendTable extends EntityDbTable<AssetDividend> {

    private static final LongKeyFactory<AssetDividend> dividendDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(AssetDividend assetDividend) {
            if (assetDividend.getDbKey() == null) {
                assetDividend.setDbKey(super.newKey(assetDividend.getId()));
            }
            return assetDividend.getDbKey();
        }

    };

    @Inject
    public AssetDividendTable(DerivedTablesRegistry derivedDbTablesRegistry,
                              DatabaseManager databaseManager) {
        super("asset_dividend", dividendDbKeyFactory, false, null,
            derivedDbTablesRegistry, databaseManager, null);
    }

    @Override
    public AssetDividend load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AssetDividend(rs, dbKey);
    }

    @Override
    public void save(Connection con, AssetDividend assetDividend) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset_dividend (id, asset_id, "
            + "amount, dividend_height, total_dividend, num_accounts, `timestamp`, height) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, assetDividend.getId());
            pstmt.setLong(++i, assetDividend.getAssetId());
            pstmt.setLong(++i, assetDividend.getAmountATMPerATU());
            pstmt.setInt(++i, assetDividend.getDividendHeight());
            pstmt.setLong(++i, assetDividend.getTotalDividend());
            pstmt.setLong(++i, assetDividend.getNumAccounts());
            pstmt.setInt(++i, assetDividend.getTimestamp());
            pstmt.setInt(++i, assetDividend.getHeight());
            pstmt.executeUpdate();
        }
    }

    public List<AssetDividend> getAssetDividends(long assetId, int from, int to) {
        return toList(getManyBy(new DbClause.LongClause("asset_id", assetId), from, to));
    }

    public AssetDividend getLastDividend(long assetId) {
        try (DbIterator<AssetDividend> dividends = getManyBy(new DbClause.LongClause("asset_id", assetId), 0, 0)) {
            if (dividends.hasNext()) {
                return dividends.next();
            }
        }
        return null;
    }

}
