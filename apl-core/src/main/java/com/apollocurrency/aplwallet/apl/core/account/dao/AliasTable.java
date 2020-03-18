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

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
    public AliasTable() {
        super("alias", aliasDbKeyFactory, false);
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
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO alias (id, account_id, alias_name, "
                + "alias_uri, timestamp, height) KEY (id, height) "
                + "VALUES (?, ?, ?, ?, ?, ?)")
        ) {
            int i = 0;
            pstmt.setLong(++i, alias.getId());
            pstmt.setLong(++i, alias.getAccountId());
            pstmt.setString(++i, alias.getAliasName());
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
