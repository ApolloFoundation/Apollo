/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
public final class AccountInfo {
    
    final long accountId;
    final DbKey dbKey;
    String name;
    String description;
    
    static final LongKeyFactory<AccountInfo> accountInfoDbKeyFactory = new LongKeyFactory<AccountInfo>("account_id") {

        @Override
        public DbKey newKey(AccountInfo accountInfo) {
            return accountInfo.dbKey;
        }

    };
       
    static final VersionedEntityDbTable<AccountInfo> accountInfoTable = new AccountInfoTable("account_info",
            accountInfoDbKeyFactory, "name,description");
    
    public AccountInfo(long accountId, String name, String description) {
        this.accountId = accountId;
        this.dbKey = accountInfoDbKeyFactory.newKey(this.accountId);
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
            accountInfoTable.insert(this);
        } else {
            accountInfoTable.delete(this);
        }
    }
  
    public static DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
        return accountInfoTable.search(query, DbClause.EMPTY_CLAUSE, from, to);
    } 
}
