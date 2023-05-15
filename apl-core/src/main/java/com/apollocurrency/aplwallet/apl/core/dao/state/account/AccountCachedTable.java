/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.CachedTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.google.common.cache.Cache;

import java.sql.SQLException;
import java.util.List;

public class AccountCachedTable extends CachedTable<Account> implements AccountTableInterface {

    public AccountCachedTable(Cache<DbKey, Account> cache, AccountTable table) {
        super(cache, table);
    }

    @Override
    public List<Account> selectAllForKey(Long id) throws SQLException {
        return accountTable().selectAllForKey(id);
    }

    @Override
    public long getTotalSupply(long creatorId) {
        return accountTable().getTotalSupply(creatorId);
    }

    @Override
    public List<Account> getTopHolders(int numberOfTopAccounts) {
        return accountTable().getTopHolders(numberOfTopAccounts);
    }

    @Override
    public long getTotalAmountOnTopAccounts(int numberOfTopAccounts) {
        return accountTable().getTotalAmountOnTopAccounts(numberOfTopAccounts);
    }

    @Override
    public long getTotalNumberOfAccounts() {
        return accountTable().getTotalNumberOfAccounts();
    }

    @Override
    public List<Account> getRecentAccounts(int limit) {
        return accountTable().getRecentAccounts(limit);
    }

    private AccountTable accountTable() {
        return (AccountTable) table;
    }
}
