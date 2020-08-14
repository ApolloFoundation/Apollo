/*
 * Copyright © 2018-2019 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;

/**
 * @author al
 */
@Singleton
public class AccountAssetTable extends VersionedDeletableEntityDbTable<AccountAsset> {

    private static final LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new AccountAssetDbKeyFactory();

    @Inject
    public AccountAssetTable(DerivedTablesRegistry derivedDbTablesRegistry,
                             DatabaseManager databaseManager) {
        super("account_asset", accountAssetDbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null);
    }

    public static DbKey newKey(long idA, long idB) {
        return accountAssetDbKeyFactory.newKey(idA, idB);
    }

    @Override
    public AccountAsset load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountAsset(rs, dbKey);
    }

    @Override
    public void save(Connection con, AccountAsset accountAsset) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE) final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
                + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest, deleted) "
                + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, accountAsset.getAccountId());
            pstmt.setLong(++i, accountAsset.getAssetId());
            pstmt.setLong(++i, accountAsset.getQuantityATU());
            pstmt.setLong(++i, accountAsset.getUnconfirmedQuantityATU());
            pstmt.setInt(++i, accountAsset.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public String defaultSort() {
        return " ORDER BY quantity DESC, account_id, asset_id ";
    }

    public int getCountByAssetId(long assetId) {
        return getCount(new DbClause.LongClause("asset_id", assetId));
    }

    public int getCountByAssetId(long assetId, int height) {
        return getCount(new DbClause.LongClause("asset_id", assetId), height);
    }

    public int getCountByAccountId(long accountId) {
        return getCount(new DbClause.LongClause("account_id", accountId));
    }

    public int getCountByAccountId(long accountId, int height) {
        return getCount(new DbClause.LongClause("account_id", accountId), height);
    }

    public List<AccountAsset> getByAccountId(long accountId, int from, int to) {
        return toList(getManyBy(new DbClause.LongClause("account_id", accountId), from, to));
    }

    public List<AccountAsset> getByAccountId(long accountId, int height, int from, int to) {
        return toList(getManyBy(new DbClause.LongClause("account_id", accountId), height, from, to));
    }

    public List<AccountAsset> getByAssetId(long assetId, int from, int to) {
        return toList(getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id "));
    }

    public List<AccountAsset> getByAssetId(long assetId, int height, int from, int to) {
        return toList(getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id "));
    }

    private static class AccountAssetDbKeyFactory extends LinkKeyFactory<AccountAsset> {

        public AccountAssetDbKeyFactory() {
            super("account_id", "asset_id");
        }

        @Override
        public DbKey newKey(AccountAsset accountAsset) {
            if (accountAsset.getDbKey() == null) {
                accountAsset.setDbKey(super.newKey(accountAsset.getAccountId(), accountAsset.getAssetId()));
            }
            return accountAsset.getDbKey();
        }
    }

}
