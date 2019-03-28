/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.model.Balances;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;

@Singleton
public class AccountService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    private Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private EthereumWalletService ethereumWalletService = CDI.current().select(EthereumWalletService.class).get();

    public Balances getAccountBalances(Account account, boolean includeEffectiveBalance, String eth, String pax){
        return getAccountBalances(account, includeEffectiveBalance, blockchain.getHeight(), eth, pax);
    }

    public Balances getAccountBalances(Account account, boolean includeEffectiveBalance, int height,  String ethAddress, String paxAddress) {
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

        try{
            if (!StringUtils.isBlank(ethAddress)) {
                balances.setBalanceETH(ethereumWalletService.getBalanceWei(ethAddress));
            }

            if (!StringUtils.isBlank(paxAddress)) {
                balances.setBalancePAX(ethereumWalletService.getPaxBalanceWei(paxAddress));
            }
        } catch (Exception ex){
            LOG.error(ex.getMessage(), ex);
        }

        return balances;
    }
}
