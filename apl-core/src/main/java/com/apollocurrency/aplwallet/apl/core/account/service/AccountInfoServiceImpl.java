/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountInfoServiceImpl implements AccountInfoService {

    @Inject @Setter
    private Blockchain blockchain;

    @Inject @Setter
    private AccountInfoTable accountInfoTable;

    @Override
    public void save(AccountInfo accountInfo) {
        if (accountInfo.getName() != null || accountInfo.getDescription() != null) {
            accountInfoTable.insert(accountInfo);
        } else {
            accountInfoTable.delete(accountInfo);
        }
    }

    @Override
    public AccountInfo getAccountInfo(Account account) {
        return accountInfoTable.get(AccountTable.newKey(account));
    }

    @Override
    public void updateAccountInfo(Account account, String name, String description) {
        name = Convert.emptyToNull(name.trim());
        description = Convert.emptyToNull(description.trim());
        AccountInfo accountInfo = getAccountInfo(account);
        if (accountInfo == null) {
            accountInfo = new AccountInfo(account.getId(), name, description, blockchain.getHeight());
        } else {
            accountInfo.setName(name);
            accountInfo.setDescription(description);
        }
        save(accountInfo);
    }


    @Override
    public List<AccountInfo> searchAccounts(String query, int from, int to) {
        List<AccountInfo> result = new ArrayList<>();
        try(DbIterator<AccountInfo> rs = accountInfoTable.searchAccounts(query, from, to)) {
            rs.forEach(result::add);
        }
        return result;
    }


}
