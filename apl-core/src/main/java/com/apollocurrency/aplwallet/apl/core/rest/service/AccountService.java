/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.utils.AccountGeneratorUtil;
import com.apollocurrency.aplwallet.apl.core.model.Balances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;

    @Inject
    public AccountService(Blockchain blockchain, BlockchainConfig blockchainConfig) {
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
            balances.setEffectiveBalanceAPL(account.getEffectiveBalanceAPL(blockchain.getHeight()));
            balances.setGuaranteedBalanceATM(account.getGuaranteedBalanceATM(blockchainConfig.getGuaranteedBalanceConfirmations(), height));
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
