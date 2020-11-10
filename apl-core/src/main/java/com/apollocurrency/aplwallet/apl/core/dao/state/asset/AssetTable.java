/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.asset;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Singleton
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
public class AssetTable extends VersionedDeletableEntityDbTable<Asset> implements SearchableTableInterface<Asset> {

    public static final LongKeyFactory<Asset> assetDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Asset asset) {
            if (asset.getDbKey() == null) {
                asset.setDbKey(super.newKey(asset.getId()));
            }
            return asset.getDbKey();
        }
    };

    @Inject
    public AssetTable(DerivedTablesRegistry derivedDbTablesRegistry,
                      DatabaseManager databaseManager,
                      FullTextConfig fullTextConfig,
                      Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("asset", assetDbKeyFactory, "name,description",
            derivedDbTablesRegistry, databaseManager, fullTextConfig, deleteOnTrimDataEvent);
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
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset "
                + "(id, account_id, `name`, description, initial_quantity, quantity, decimals, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), account_id = VALUES(account_id), `name` = VALUES(`name`), "
                + "description = VALUES(description), initial_quantity = VALUES(initial_quantity),"
                + "quantity = VALUES(quantity), decimals = VALUES(decimals), height = VALUES(height), "
                + "latest = TRUE , deleted = FALSE ",
                Statement.RETURN_GENERATED_KEYS)
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
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    asset.setDbId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public final DbIterator<Asset> search(String query, DbClause dbClause, int from, int to) {
        throw new UnsupportedOperationException("Call service, should be implemented by service");
    }

    @Override
    public final DbIterator<Asset> search(String query, DbClause dbClause, int from, int to, String sort) {
        throw new UnsupportedOperationException("Call service, should be implemented by service");
    }



}
