/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.account.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.account.AccountsCountDto;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Account statistic information.
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
        this.accountService = Objects.requireNonNull(accountService, "accountService is NULL");
    }

    public AccountsCountDto getAccountsStatistic(int numberOfAccounts) {
        log.trace("start getAccountsStatistic = {}", numberOfAccounts);
        AccountsCountDto dto = new AccountsCountDto();
        dto.totalSupply = accountService.getTotalSupply();
        log.trace("getAccountsStatistic dto.totalSupply= {}", dto.totalSupply);
        dto.totalNumberOfAccounts = accountService.getTotalNumberOfAccounts();
        log.trace("getAccountsStatistic dto.totalNumberOfAccounts= {}", dto.totalNumberOfAccounts);
        dto.numberOfTopAccounts = numberOfAccounts;
        dto.totalAmountOnTopAccounts = accountService.getTotalAmountOnTopAccounts(numberOfAccounts);
        log.trace("Found totalAmountOnTopAccounts = {} by numberOfAccounts={}", dto.totalAmountOnTopAccounts, numberOfAccounts);
        List<Account> topHoldersList = accountService.getTopHolders(numberOfAccounts);
        log.trace("topHoldersList = [{}]", topHoldersList.size());
        int index = 0;
        for (Account account : topHoldersList) {
            long start = System.currentTimeMillis();
            log.trace("accountJson START for  account={}, index={}", account.getId(), index);
            AccountEffectiveBalanceDto accountJson = accountBalance(account, false, blockchain.getHeight());
            log.trace("accountJson = {}, index={} in '{}' msec, DONE", accountJson, index, System.currentTimeMillis() - start);
            putAccountNameInfo(accountJson, account.getId(), false);
            dto.topHolders.add(accountJson);
            index++;
        }
        return dto;
    }

    private AccountEffectiveBalanceDto accountBalance(Account account, boolean includeEffectiveBalance, int height) {
        AccountEffectiveBalanceDto json = new AccountEffectiveBalanceDto();
        if (account != null) {
            json.setBalanceATM(account.getBalanceATM());
            json.setUnconfirmedBalanceATM(account.getUnconfirmedBalanceATM());
            json.setForgedBalanceATM(account.getForgedBalanceATM());
            if (includeEffectiveBalance) {
                json.effectiveBalanceAPL = accountService.getEffectiveBalanceAPL(account, height, false);
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
