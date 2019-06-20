/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountAsset;
import com.apollocurrency.aplwallet.apl.core.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import com.apollocurrency.aplwallet.apl.core.utils.AccountGeneratorUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;

@Singleton
public class AccountServiceImpl implements AccountService {

    @Override
    public Account getAccount(String account) {
        long accountId = Convert.parseAccountId(account);
        if (accountId == 0){
            return null;
        }
        return Account.getAccount(accountId);
    }

    @Override
    public List<Account> getLessors(Account account) {
        DbIterator<Account> iterator = account.getLessors();
        LinkedList<Account> result = new LinkedList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }

    @Override
    public List<Account> getLessors(Account account, int height) {
        DbIterator<Account> iterator = account.getLessors(height);
        LinkedList<Account> result = new LinkedList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }

    @Override
    public List<AccountAsset> getAccountAssets(Account account) {
        DbIterator<AccountAsset> iterator = account.getAssets(0, -1);
        LinkedList<AccountAsset> result = new LinkedList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }

    @Override
    public List<AccountCurrency> getAccountCurrencies(Account account) {
        DbIterator<AccountCurrency> iterator = account.getCurrencies(0, -1);
        LinkedList<AccountCurrency> result = new LinkedList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }

    @Override
    public ApolloFbWallet generateUserAccounts(byte[] secretApl) {
        ApolloFbWallet apolloWallet = new ApolloFbWallet();
        AplWalletKey aplAccount = secretApl == null ? AccountGeneratorUtil.generateApl() : new AplWalletKey(secretApl);

        apolloWallet.addAplKey(aplAccount);
        apolloWallet.addEthKey(AccountGeneratorUtil.generateEth());
        return apolloWallet;
    }
}
