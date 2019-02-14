/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public final class AccountProperty {
    
    private final long id;
    final DbKey dbKey;
    private final long recipientId;
    private final long setterId;
    private String property;
    String value;
    
    static final LongKeyFactory<AccountProperty> accountPropertyDbKeyFactory = new LongKeyFactory<AccountProperty>("id") {

        @Override
        public DbKey newKey(AccountProperty accountProperty) {
            return accountProperty.dbKey;
        }

    };
    
    public AccountProperty(long id, long recipientId, long setterId, String property, String value) {
        this.id = id;
        this.dbKey = accountPropertyDbKeyFactory.newKey(this.id);
        this.recipientId = recipientId;
        this.setterId = setterId;
        this.property = property;
        this.value = value;
    }

    public AccountProperty(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.recipientId = rs.getLong("recipient_id");
        long setterIdL = rs.getLong("setter_id");
        this.setterId = setterIdL == 0 ? recipientId : setterIdL;
        this.property = rs.getString("property");
        this.value = rs.getString("value");
    }

    public void save(Connection con) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_property " + "(id, recipient_id, setter_id, property, value, height, latest) " + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.recipientId);
            DbUtils.setLongZeroToNull(pstmt, ++i, this.setterId != this.recipientId ? this.setterId : 0);
            DbUtils.setString(pstmt, ++i, this.property);
            DbUtils.setString(pstmt, ++i, this.value);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public long getSetterId() {
        return setterId;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }
    
}
