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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.alias;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
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
import java.util.Locale;
import java.util.Objects;

@Singleton
public class AliasTable extends VersionedDeletableEntityDbTable<Alias> {

    private static final LongKeyFactory<Alias> aliasDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Alias alias) {
            if (alias.getDbKey() == null) {
                alias.setDbKey(super.newKey(alias.getId()));
            }
            return alias.getDbKey();
        }
    };

    @Inject
    public AliasTable(DerivedTablesRegistry derivedDbTablesRegistry,
                      DatabaseManager databaseManager,
                      Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super("alias", aliasDbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    @Override
    public Alias load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new Alias(rs, dbKey);
    }

    @Override
    public void save(Connection con, Alias alias) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias (id, account_id, alias_name, "
                + "alias_name_lower, alias_uri, `timestamp`, height) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), account_id = VALUES(account_id), alias_name = VALUES(alias_name), "
                + "alias_name_lower = VALUES(alias_name_lower), alias_uri = VALUES(alias_uri), "
                + "`timestamp` = VALUES(`timestamp`), height = VALUES(height)")
        ) {
            Objects.requireNonNull(alias);
            final String aliasName = alias.getAliasName();
            Objects.requireNonNull(aliasName);

            int i = 0;
            pstmt.setLong(++i, alias.getId());
            pstmt.setLong(++i, alias.getAccountId());
            pstmt.setString(++i, alias.getAliasName());
            pstmt.setString(++i, aliasName.toLowerCase(Locale.US));
            pstmt.setString(++i, alias.getAliasURI());
            pstmt.setInt(++i, alias.getTimestamp());
            pstmt.setInt(++i, alias.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public String defaultSort() {
        return " ORDER BY alias_name_lower ";
    }

    public Alias getAlias(long id) {
        return get(aliasDbKeyFactory.newKey(id));
    }
}
