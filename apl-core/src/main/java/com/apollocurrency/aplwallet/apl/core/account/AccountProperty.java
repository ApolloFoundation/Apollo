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
    
    final long id;
    final DbKey dbKey;
    final long recipientId;
    final long setterId;
    String property;
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
