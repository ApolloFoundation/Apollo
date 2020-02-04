/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Order;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import com.apollocurrency.aplwallet.apl.core.utils.AccountGeneratorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AccountBalanceService {

    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;
    private AccountService accountService;

    @Inject
    public AccountBalanceService(AccountService accountService, Blockchain blockchain, BlockchainConfig blockchainConfig) {
        this.accountService = accountService;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
    }

    public Balances getAccountBalances(Account account, boolean includeEffectiveBalance){
        return getAccountBalances(account, includeEffectiveBalance, blockchain.getHeight());
    }

    public Balances getAccountBalances(Account account, boolean includeEffectiveBalance, int height) {
        if(account == null){
            return null;
        }
        Balances balances = new Balances();

        balances.setBalanceATM(account.getBalanceATM());
        balances.setUnconfirmedBalanceATM(account.getUnconfirmedBalanceATM());
        balances.setForgedBalanceATM(account.getForgedBalanceATM());

        if (includeEffectiveBalance) {
            balances.setEffectiveBalanceAPL(accountService.getEffectiveBalanceAPL(account, blockchain.getHeight(), false));
            balances.setGuaranteedBalanceATM(accountService.getGuaranteedBalanceATM(account, blockchainConfig.getGuaranteedBalanceConfirmations(), height));
        }

        return balances;
    }

    public ApolloFbWallet generateUserAccounts(byte[] secretApl) {
        ApolloFbWallet apolloWallet = new ApolloFbWallet();
        AplWalletKey aplAccount = secretApl == null ? AccountGeneratorUtil.generateApl() : new AplWalletKey(secretApl);

        apolloWallet.addAplKey(aplAccount);
        apolloWallet.addEthKey(AccountGeneratorUtil.generateEth());
        return apolloWallet;
    }

    public List<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to){
        List<Order.Ask> orders = new ArrayList<>();
        try(DbIterator<Order.Ask> askOrders = Order.Ask.getAskOrdersByAccount(accountId, from, to)){
            askOrders.forEach(orders::add);
        }
        return orders;
    }

    public List<Order.Ask> getAskOrdersByAccountAsset(long accountId, long assetId, int from, int to){
        List<Order.Ask> orders = new ArrayList<>();
        try(DbIterator<Order.Ask> askOrders = Order.Ask.getAskOrdersByAccountAsset(accountId, assetId, from, to)){
            askOrders.forEach(orders::add);
        }
        return orders;
    }

}
