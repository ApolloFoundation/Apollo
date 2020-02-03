/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
@ToString(callSuper = true)
@Getter @Setter
public final class AccountProperty extends VersionedDerivedEntity{

    final long id;
    final long recipientId;
    final long setterId;
    String property;
    String value;

    public AccountProperty(long id, long recipientId, long setterId, String property, String value, int height) {
        super(null, height);
        this.id = id;
        this.recipientId = recipientId;
        this.setterId = setterId;
        this.property = property;
        this.value = value;
    }

    public AccountProperty(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.recipientId = rs.getLong("recipient_id");
        long setterIdL = rs.getLong("setter_id");
        this.setterId = setterIdL == 0 ? recipientId : setterIdL;
        this.property = rs.getString("property");
        this.value = rs.getString("value");
        setDbKey(dbKey);
    }

}
