/*
 * Copyright © 2018-2019 Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
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

/**
 * @author al
 */
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Singleton
public class AccountInfoTable extends VersionedDeletableEntityDbTable<AccountInfo> implements SearchableTableInterface<AccountInfo> {

    private static final LongKeyFactory<AccountInfo> accountInfoDbKeyFactory = new LongKeyFactory<AccountInfo>("account_id") {
        @Override
        public DbKey newKey(AccountInfo accountInfo) {
            if (accountInfo.getDbKey() == null) {
                accountInfo.setDbKey(super.newKey(accountInfo.getAccountId()));
            }
            return accountInfo.getDbKey();
        }
    };

    @Inject
    public AccountInfoTable(DerivedTablesRegistry derivedDbTablesRegistry,
                            DatabaseManager databaseManager,
                            FullTextConfig fullTextConfig,
                            Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("account_info",
            accountInfoDbKeyFactory, "name,description",
            derivedDbTablesRegistry, databaseManager, fullTextConfig, deleteOnTrimDataEvent);
    }

    public static DbKey newKey(long id) {
        return accountInfoDbKeyFactory.newKey(id);
    }

    @Override
    public AccountInfo load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountInfo(rs, dbKey);
    }

    @Override
    public void save(Connection con, AccountInfo accountInfo) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            final PreparedStatement pstmt = con.prepareStatement("INSERT INTO account_info "
                + "(account_id, `name`, description, height, latest, deleted) "
                + "VALUES (?, ?, ?, ?, TRUE, FALSE) "
                + "ON DUPLICATE KEY UPDATE account_id = VALUES(account_id), `name` = VALUES(`name`), "
                + "description = VALUES(description), height = VALUES(height), latest = TRUE, deleted = FALSE")
        ) {
            int i = 0;
            pstmt.setLong(++i, accountInfo.getAccountId());
            DbUtils.setString(pstmt, ++i, accountInfo.getName());
            DbUtils.setString(pstmt, ++i, accountInfo.getDescription());
            pstmt.setInt(++i, accountInfo.getHeight());
            pstmt.executeUpdate();
        }
    }

    public DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
        return search(query, DbClause.EMPTY_CLAUSE, from, to);
    }

    @Override
    public final DbIterator<AccountInfo> search(String query, DbClause dbClause, int from, int to) {
        return search(query, dbClause, from, to, " ORDER BY ft.score DESC ");
    }

    @Override
    public final DbIterator<AccountInfo> search(String query, DbClause dbClause, int from, int to, String sort) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement("SELECT " + table + ".*, ft.score FROM " + table +
                ", ftl_search('PUBLIC', '" + table + "', ?, 2147483647, 0) ft "
                + " WHERE " + table + ".db_id = ft.keys[1] "
                + (multiversion ? " AND " + table + ".latest = TRUE " : " ")
                + " AND " + dbClause.getClause() + sort
                + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setString(++i, query);
            i = dbClause.set(pstmt, ++i);
            i = DbUtils.setLimits(i, pstmt, from, to);
            return getManyBy(con, pstmt, true);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

}
