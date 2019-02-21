/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public class AccountPropertyTable extends VersionedEntityDbTable<AccountProperty> {
    
    private static final LongKeyFactory<AccountProperty> accountPropertyDbKeyFactory = new LongKeyFactory<AccountProperty>("id") {

        @Override
        public DbKey newKey(AccountProperty accountProperty) {
            return accountProperty.dbKey;
        }

    };
    private static final AccountPropertyTable accountPropertyTable = new AccountPropertyTable(); 
    
    public static AccountPropertyTable getInstance(){
        return accountPropertyTable;
    }
    
    public static DbKey newKey(long id){
        return accountPropertyDbKeyFactory.newKey(id);
    }
    
    private AccountPropertyTable() {
        super("account_property", accountPropertyDbKeyFactory);
    }

    @Override
    protected AccountProperty load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountProperty(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountProperty accountProperty) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_property " + "(id, recipient_id, setter_id, property, value, height, latest) " + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountProperty.id);
            pstmt.setLong(++i, accountProperty.recipientId);
            DbUtils.setLongZeroToNull(pstmt, ++i, accountProperty.setterId != accountProperty.recipientId ? accountProperty.setterId : 0);
            DbUtils.setString(pstmt, ++i, accountProperty.property);
            DbUtils.setString(pstmt, ++i, accountProperty.value);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }
    
    public static AccountProperty getProperty(long propertyId) {
        return accountPropertyTable.get(AccountPropertyTable.newKey(propertyId));
    }

    public static DbIterator<AccountProperty> getProperties(long recipientId, long setterId, String property, int from, int to) {
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
        return accountPropertyTable.getManyBy(dbClause, from, to, " ORDER BY property ");
    }

    public static AccountProperty getProperty(long recipientId, String property) {
        return getProperty(recipientId, property, recipientId);
    }

    public static AccountProperty getProperty(long recipientId, String property, long setterId) {
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
        return accountPropertyTable.getBy(dbClause);
    }

    
}
