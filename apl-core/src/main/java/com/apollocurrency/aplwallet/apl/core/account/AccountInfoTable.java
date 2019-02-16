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
public class AccountInfoTable extends VersionedEntityDbTable<AccountInfo> {

    private static final LongKeyFactory<AccountInfo> accountInfoDbKeyFactory = new LongKeyFactory<AccountInfo>("account_id") {

        @Override
        public DbKey newKey(AccountInfo accountInfo) {
            return accountInfo.dbKey;
        }

    };
       
    private  static final AccountInfoTable accountInfoTable = new AccountInfoTable();  
    public static DbKey newKey(long id){
        return accountInfoDbKeyFactory.newKey(id);
    }
    
    public static AccountInfoTable getInstance(){
        return accountInfoTable;
    }
    
    public AccountInfoTable() {
        super("account_info",
            accountInfoDbKeyFactory, "name,description");
    }

    @Override
    protected AccountInfo load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AccountInfo(rs, dbKey);
    }

    @Override
    protected void save(Connection con, AccountInfo accountInfo) throws SQLException {
        try (final PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_info " + "(account_id, name, description, height, latest) " + "KEY (account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountInfo.accountId);
            DbUtils.setString(pstmt, ++i, accountInfo.name);
            DbUtils.setString(pstmt, ++i, accountInfo.description);
            pstmt.setInt(++i, Account.blockchain.getHeight());
            pstmt.executeUpdate();
        }
    }
   

    public static DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
        return accountInfoTable.search(query, DbClause.EMPTY_CLAUSE, from, to);
    } 
}
