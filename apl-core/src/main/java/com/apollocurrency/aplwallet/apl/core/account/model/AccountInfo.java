/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDeletableEntity;
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
public final class AccountInfo extends VersionedDeletableEntity {

    private final long accountId;
    private String name;
    private String description;

    public AccountInfo(long accountId, String name, String description, int height) {
        super(null, height);
        this.accountId = accountId;
        this.name = name;
        this.description = description;
    }

    public AccountInfo(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        setDbKey(dbKey);
    }
}
