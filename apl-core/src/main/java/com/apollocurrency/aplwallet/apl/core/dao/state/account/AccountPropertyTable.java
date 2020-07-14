/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountProperty;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author al
 */
@Singleton
public class AccountPropertyTable extends VersionedDeletableEntityDbTable<AccountProperty> {

    private static final LongKeyFactory<AccountProperty> accountPropertyDbKeyFactory = new LongKeyFactory<AccountProperty>("id") {
        @Override
        public DbKey newKey(AccountProperty accountProperty) {
            if (accountProperty.getDbKey() == null) {
                accountProperty.setDbKey(super.newKey(accountProperty.getId()));
            }
            return accountProperty.getDbKey();
        }
    };

    private AccountPropertyTable() {
        super("account_property", accountPropertyDbKeyFactory, false);
    }

    public static DbKey newKey(long id) {
        return accountPropertyDbKeyFactory.newKey(id);
    }

    @Override
    public AccountProperty load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountProperty(rs, dbKey);
    }

    @Override
    public void save(Connection con, AccountProperty accountProperty) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE) final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_property " + "(id, recipient_id, setter_id, property, \"VALUE\", height, latest, deleted) " + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE, FALSE)")
        ) {
            int i = 0;
            pstmt.setLong(++i, accountProperty.getId());
            pstmt.setLong(++i, accountProperty.getRecipientId());
            DbUtils.setLongZeroToNull(pstmt, ++i, accountProperty.getSetterId() != accountProperty.getRecipientId() ? accountProperty.getSetterId() : 0);
            DbUtils.setString(pstmt, ++i, accountProperty.getProperty());
            DbUtils.setString(pstmt, ++i, accountProperty.getValue());
            pstmt.setInt(++i, accountProperty.getHeight());
            pstmt.executeUpdate();
        }
    }

    public DbIterator<AccountProperty> getProperties(long recipientId, long setterId, String property, int from, int to) {
        if (recipientId == 0 && setterId == 0) {
            throw new IllegalArgumentException("At least one of recipientId and setterId must be specified");
        }
        DbClause dbClause = null;
        if (setterId == recipientId) {
            dbClause = new DbClause.NullClause("setter_id");
        } else if (setterId != 0) {
            dbClause = new DbClause.LongClause("setter_id", setterId);
        }
        if (recipientId != 0) {
            if (dbClause != null) {
                dbClause = dbClause.and(new DbClause.LongClause("recipient_id", recipientId));
            } else {
                dbClause = new DbClause.LongClause("recipient_id", recipientId);
            }
        }
        if (property != null) {
            dbClause = dbClause.and(new DbClause.StringClause("property", property));
        }
        return getManyBy(dbClause, from, to, " ORDER BY property ");
    }

    public AccountProperty getProperty(long recipientId, String property, long setterId) {
        if (recipientId == 0 || setterId == 0) {
            throw new IllegalArgumentException("Both recipientId and setterId must be specified");
        }
        DbClause dbClause = new DbClause.LongClause("recipient_id", recipientId);
        dbClause = dbClause.and(new DbClause.StringClause("property", property));
        if (setterId != recipientId) {
            dbClause = dbClause.and(new DbClause.LongClause("setter_id", setterId));
        } else {
            dbClause = dbClause.and(new DbClause.NullClause("setter_id"));
        }
        return getBy(dbClause);
    }


}
