/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.entity.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDeletableEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author al
 */
@ToString(callSuper = true)
@Getter
@Setter
public final class AccountProperty extends VersionedDeletableEntity {

    private final long id;
    private final long recipientId;
    private final long setterId;
    private String property;
    private String value;

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
