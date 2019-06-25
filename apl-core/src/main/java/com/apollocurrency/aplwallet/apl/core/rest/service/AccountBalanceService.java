/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import com.apollocurrency.aplwallet.apl.core.utils.AccountGeneratorUtil;

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

    public Balances getAccountBalances(AccountEntity account, boolean includeEffectiveBalance){
        return getAccountBalances(account, includeEffectiveBalance, blockchain.getHeight());
    }

    public Balances getAccountBalances(AccountEntity account, boolean includeEffectiveBalance, int height) {
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
}
