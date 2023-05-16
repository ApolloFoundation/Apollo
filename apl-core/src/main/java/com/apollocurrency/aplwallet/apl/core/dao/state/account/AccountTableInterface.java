/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;

import java.sql.SQLException;
import java.util.List;

public interface AccountTableInterface extends EntityDbTableInterface<Account> {
    LongKeyFactory<Account> accountDbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Account account) {
            if (account.getDbKey() == null) {
                account.setDbKey(super.newKey(account.getId()));
            }
            return account.getDbKey();
        }
    };

    static DbKey newKey(long id) {
        return accountDbKeyFactory.newKey(id);
    }

    static DbKey newKey(Account a) {
        return accountDbKeyFactory.newKey(a);
    }


    List<Account> selectAllForKey(Long id) throws SQLException;

    long getTotalSupply(long creatorId);

    List<Account> getTopHolders(int numberOfTopAccounts);

    long getTotalAmountOnTopAccounts(int numberOfTopAccounts);

    long getTotalNumberOfAccounts();

    List<Account> getRecentAccounts(int limit);

}
