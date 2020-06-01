/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.operation.account;

import com.apollocurrency.aplwallet.apl.core.entity.operation.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.operation.account.AccountInfo;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface AccountInfoService {

    void update(AccountInfo accountInfo);

    AccountInfo getAccountInfo(Account account);

    void updateAccountInfo(Account account, String name, String description);

    List<AccountInfo> searchAccounts(String query, int from, int to);
}
