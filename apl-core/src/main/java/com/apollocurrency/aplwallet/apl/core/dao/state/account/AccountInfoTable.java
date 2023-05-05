/*
 * Copyright © 2018-2019 Apollo Foundation.
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableMarkerInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author al
 */
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Singleton
public class AccountInfoTable extends VersionedDeletableEntityDbTable<AccountInfo> implements SearchableTableMarkerInterface<AccountInfo> {

    public static final String TABLE_NAME = "account_info";
    public static final String FULL_TEXT_SEARCH_COLUMNS = "name,description";


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
    public AccountInfoTable(DatabaseManager databaseManager,
                            Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME,
            accountInfoDbKeyFactory, FULL_TEXT_SEARCH_COLUMNS,
                databaseManager, fullTextOperationDataEvent);
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
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE) final PreparedStatement pstmt = con.prepareStatement("INSERT INTO account_info "
                    + "(account_id, `name`, description, height, latest, deleted) "
                    + "VALUES (?, ?, ?, ?, TRUE, FALSE) "
                    + "ON DUPLICATE KEY UPDATE account_id = VALUES(account_id), `name` = VALUES(`name`), "
                    + "description = VALUES(description), height = VALUES(height), latest = TRUE, deleted = FALSE",
                Statement.RETURN_GENERATED_KEYS)
        ) {
            int i = 0;
            pstmt.setLong(++i, accountInfo.getAccountId());
            DbUtils.setString(pstmt, ++i, accountInfo.getName());
            DbUtils.setString(pstmt, ++i, accountInfo.getDescription());
            pstmt.setInt(++i, accountInfo.getHeight());
            pstmt.executeUpdate();
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    accountInfo.setDbId(rs.getLong(1));
                }
            }
        }
    }

}
