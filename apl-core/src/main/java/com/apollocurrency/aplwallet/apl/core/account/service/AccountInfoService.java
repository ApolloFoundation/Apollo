package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountInfo;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountInfoService {

    void save(AccountInfo accountInfo);

    AccountInfo getAccountInfo(AccountEntity account);

    void updateAccountInfo(AccountEntity account, String name, String description);

    List<AccountInfo> searchAccounts(String query, int from, int to);
}
