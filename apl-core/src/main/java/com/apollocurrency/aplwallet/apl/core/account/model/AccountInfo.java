/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
@Getter @Setter
public final class AccountInfo extends VersionedDerivedEntity {

    final long accountId;
    String name;
    String description;
    
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
