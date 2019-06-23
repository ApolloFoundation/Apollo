package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountInfo;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Singleton
public class AccountInfoServiceImpl implements AccountInfoService {

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
    public AccountInfo getAccountInfo(AccountEntity account) {
        return accountInfoTable.get(AccountTable.newKey(account));
    }

    @Override
    public void updateAccountInfo(AccountEntity account, String name, String description) {
        name = Convert.emptyToNull(name.trim());
        description = Convert.emptyToNull(description.trim());
        AccountInfo accountInfo = getAccountInfo(account);
        if (accountInfo == null) {
            accountInfo = new AccountInfo(account.getId(), name, description);
        } else {
            accountInfo.setName(name);
            accountInfo.setDescription(description);
        }
        accountInfo.save();
    }


    @Override
    public List<AccountInfo> searchAccounts(String query, int from, int to) {
        List<AccountInfo> result = new ArrayList<>();
        try(DbIterator<AccountInfo> rs = accountInfoTable.search(query, DbClause.EMPTY_CLAUSE, from, to)) {
            rs.forEach(accountInfo -> result.add(accountInfo));
        }
        return result;
    }


}
