/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.account.model;

import com.apollocurrency.aplwallet.apl.core.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
@Getter @Setter
public final class AccountInfo {
    //TODO remove the unneeded public scope
    public final long accountId;
    public final DbKey dbKey;
    public String name;
    public String description;
    
    public AccountInfo(long accountId, String name, String description) {
        this.accountId = accountId;
        this.dbKey = AccountInfoTable.newKey(this.accountId);
        this.name = name;
        this.description = description;
    }

    public AccountInfo(ResultSet rs, DbKey dbKey) throws SQLException {
        this.accountId = rs.getLong("account_id");
        this.dbKey = dbKey;
        this.name = rs.getString("name");
        this.description = rs.getString("description");
    }

    public void save() {
        if (this.name != null || this.description != null) {
            AccountInfoTable.getInstance().insert(this);
        } else {
            AccountInfoTable.getInstance().delete(this);
        }
    }
}
