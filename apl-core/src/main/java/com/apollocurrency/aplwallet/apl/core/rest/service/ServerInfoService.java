/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.ApolloX509Info;
import com.apollocurrency.aplwallet.api.dto.GeneratorInfo;
import com.apollocurrency.aplwallet.api.dto.info.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.info.AccountsCountDto;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 *
 * @author alukin@gmail.com
 */
@Slf4j
@Singleton
public class ServerInfoService {
    private DatabaseManager databaseManager;
    private AccountTable accountTable;
    private BlockchainConfig blockchainConfig;
    private Blockchain blockchain;

    @Inject
    public ServerInfoService(DatabaseManager databaseManager, AccountTable accTable,
                             BlockchainConfig blockchainConfig, Blockchain blockchain) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.accountTable = Objects.requireNonNull(accTable, "accTable is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
    }

    public ApolloX509Info getX509Info(){
        ApolloX509Info res = new ApolloX509Info();
        res.id = "No ID yet available";
        return res;
    }

    public List<GeneratorInfo> getActiveForgers(boolean showBallances) {
        List<GeneratorInfo> res = new ArrayList<>();
        List<Generator> forgers = Generator.getSortedForgers();
        for (Generator g : forgers) {
            GeneratorInfo gi = new GeneratorInfo();
            gi.setAccount(Convert.defaultRsAccount(g.getAccountId()));
            gi.setDeadline(g.getDeadline());
            gi.setHitTime(g.getHitTime());
            if (showBallances) {
                gi.setEffectiveBalanceAPL(0L);
            }
        }
        return res;
    }

    public AccountsCountDto getAccountsStatistic(int numberOfAccounts) {
        AccountsCountDto dto = new AccountsCountDto();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
//            long totalSupply = AccountTable.getTotalSupply(con);
            long totalSupply = accountTable.getTotalSupply(con);
//            long totalAccounts = AccountTable.getTotalNumberOfAccounts(con);
            long totalAccounts = accountTable.getTotalNumberOfAccounts(con);
//            long totalAmountOnTopAccounts = AccountTable.getTotalAmountOnTopAccounts(con, numberOfAccounts);
            long totalAmountOnTopAccounts = accountTable.getTotalAmountOnTopAccounts(con, numberOfAccounts);
//            DbIterator<Account> topHolders = accountTable.getTopHolders(con, numberOfAccounts);
//            for (Account account : topHolders) {
//                return accounts(topHolders, totalAmountOnTopAccounts, totalSupply, totalAccounts, numberOfAccounts);
//            }
            try(DbIterator<Account> topHoldersIterator = accountTable.getTopHolders(con, numberOfAccounts)) {
                accounts(dto, topHoldersIterator, totalAmountOnTopAccounts, totalSupply, totalAccounts, numberOfAccounts);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return dto;
    }

    private AccountsCountDto accounts(AccountsCountDto dto, DbIterator<Account> topAccountsIterator,
                              long totalAmountOnTopAccounts, long totalSupply,
                              long totalAccounts, int numberOfAccounts) {
//        JSONObject result = new JSONObject();
//        result.put("totalSupply", totalSupply);
        dto.totalSupply = totalSupply;
        dto.totalNumberOfAccounts = totalAccounts;
        dto.numberOfTopAccounts = numberOfAccounts;
        dto.totalAmountOnTopAccounts = totalAmountOnTopAccounts;
//        JSONArray holders = new JSONArray();
        while (topAccountsIterator.hasNext()) {
            Account account = topAccountsIterator.next();
//            JSONObject accountJson = JSONData.accountBalance(account, false);
            AccountEffectiveBalanceDto accountJson = accountBalance(account, false, blockchain.getHeight());
//            JSONData.putAccount(accountJson, "account", account.getId(), false);
            putAccountNameInfo(accountJson, account.getId(), false);
            dto.topHolders.add(accountJson);
        }
//        result.put("topHolders", holders);
        return dto;
    }
    private AccountEffectiveBalanceDto accountBalance(Account account, boolean includeEffectiveBalance, int height) {
        AccountEffectiveBalanceDto json = new AccountEffectiveBalanceDto();
        if (account != null) {
/*
            json.balanceATM = 0;
            json.put("unconfirmedBalanceATM", "0");
            json.put("forgedBalanceATM", "0");
            if (includeEffectiveBalance) {
                json.put("effectiveBalanceAPL", "0");
                json.put("guaranteedBalanceATM", "0");
            }
        } else {
*/
            json.balanceATM = account.getBalanceATM();
            json.unconfirmedBalanceATM = account.getUnconfirmedBalanceATM();
            json.forgedBalanceATM = account.getForgedBalanceATM();
            if (includeEffectiveBalance) {
                json.effectiveBalanceAPL = account.getEffectiveBalanceAPL(height, false);
                json.guaranteedBalanceATM = account.getGuaranteedBalanceATM(
                    blockchainConfig.getGuaranteedBalanceConfirmations(), height);
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
