/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.account.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.account.AccountsCountDto;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
@Singleton
public class AccountStatisticsService {
    private BlockchainConfig blockchainConfig;
    private Blockchain blockchain;
    private AccountService accountService;

    @Inject
    public AccountStatisticsService(BlockchainConfig blockchainConfig,
                                    Blockchain blockchain,
                                    AccountService accountService) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.accountService = Objects.requireNonNull(accountService,"accountService is NULL");
    }

    public AccountsCountDto getAccountsStatistic(int numberOfAccounts) {
        AccountsCountDto dto = new AccountsCountDto();
        dto.totalSupply = accountService.getTotalSupply();
        dto.totalNumberOfAccounts = accountService.getTotalNumberOfAccounts();
        dto.numberOfTopAccounts = numberOfAccounts;
        dto.totalAmountOnTopAccounts = accountService.getTotalAmountOnTopAccounts(numberOfAccounts);
        List<Account> topHoldersIterator = accountService.getTopHolders(numberOfAccounts);
        composeAccountCountDto(dto, topHoldersIterator);
        return dto;
    }

    private void composeAccountCountDto(AccountsCountDto dto, List<Account> topAccountsIterator) {
        while (topAccountsIterator.iterator().hasNext()) {
            Account account = topAccountsIterator.iterator().next();
            AccountEffectiveBalanceDto accountJson = accountBalance(account, false, blockchain.getHeight());
            putAccountNameInfo(accountJson, account.getId(), false);
            dto.topHolders.add(accountJson);
        }
    }

    private AccountEffectiveBalanceDto accountBalance(Account account, boolean includeEffectiveBalance, int height) {
        AccountEffectiveBalanceDto json = new AccountEffectiveBalanceDto();
        if (account != null) {
            json.setBalanceATM(account.getBalanceATM());
            json.setUnconfirmedBalanceATM(account.getUnconfirmedBalanceATM());
            json.setForgedBalanceATM(account.getForgedBalanceATM());
            if (includeEffectiveBalance) {
                json.effectiveBalanceAPL = accountService.getEffectiveBalanceAPL(account , height, false);
                json.setEffectiveBalanceAPL(accountService.getGuaranteedBalanceATM(account,
                    blockchainConfig.getGuaranteedBalanceConfirmations(), height));
            }
        }
        return json;
    }

    private void putAccountNameInfo(AccountEffectiveBalanceDto json, long accountId, boolean isPrivate) {
        json.account = Long.toUnsignedString(accountId);
        if (isPrivate) {
            Random random = new Random();
            accountId = random.nextLong();
        }
        json.accountRS = Convert2.rsAccount(blockchainConfig.getAccountPrefix(), accountId);
    }

}
