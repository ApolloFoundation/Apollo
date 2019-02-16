/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public final class AccountInfo {
    
    final long accountId;
    final DbKey dbKey;
    String name;
    String description;
    
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

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    void save() {
        if (this.name != null || this.description != null) {
            AccountInfoTable.getInstance().insert(this);
        } else {
            AccountInfoTable.getInstance().delete(this);
        }
    }
  
    public static DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
        return AccountInfoTable.getInstance().search(query, DbClause.EMPTY_CLAUSE, from, to);
    } 
}
