/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.Balances;

import java.util.List;

public interface AccountService {

    Account getAccount(String accountId);

    List<Account> getLessors(Account account);

    List<Account> getLessors(Account account, int height);

    List<AccountAsset> getAccountAssets(Account account);

    List<AccountCurrency> getAccountCurrencies(Account account);

    ApolloFbWallet generateUserAccounts(byte[] secretApl);
}
