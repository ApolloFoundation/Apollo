/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.ApolloX509Info;
import com.apollocurrency.aplwallet.api.dto.GeneratorInfo;
import com.apollocurrency.aplwallet.api.dto.info.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.info.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
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
    private PropertiesHolder propertiesHolder;
    private BlockchainProcessor blockchainProcessor;
    private PeersService peersService;
    private TimeService timeService;

    @Inject
    public ServerInfoService(DatabaseManager databaseManager, AccountTable accTable,
                             BlockchainConfig blockchainConfig, Blockchain blockchain,
                             PropertiesHolder propertiesHolder,
                             BlockchainProcessor blockchainProcessor,
                             PeersService peersService, TimeService timeService) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.accountTable = Objects.requireNonNull(accTable, "accTable is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder,"propertiesHolder is NULL");
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor,"blockchainProcessor is NULL");
        this.peersService = Objects.requireNonNull(peersService,"peersService is NULL");
        this.timeService = Objects.requireNonNull(timeService,"timeService is NULL");
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
            long totalSupply = accountTable.getTotalSupply(con);
            long totalAccounts = accountTable.getTotalNumberOfAccounts(con);
            long totalAmountOnTopAccounts = accountTable.getTotalAmountOnTopAccounts(con, numberOfAccounts);
            try(DbIterator<Account> topHoldersIterator = accountTable.getTopHolders(con, numberOfAccounts)) {
                composeAccountCountDto(dto, topHoldersIterator, totalAmountOnTopAccounts, totalSupply, totalAccounts, numberOfAccounts);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return dto;
    }

    private AccountsCountDto composeAccountCountDto(AccountsCountDto dto, DbIterator<Account> topAccountsIterator,
                                                    long totalAmountOnTopAccounts, long totalSupply,
                                                    long totalAccounts, int numberOfAccounts) {
        dto.totalSupply = totalSupply;
        dto.totalNumberOfAccounts = totalAccounts;
        dto.numberOfTopAccounts = numberOfAccounts;
        dto.totalAmountOnTopAccounts = totalAmountOnTopAccounts;
        while (topAccountsIterator.hasNext()) {
            Account account = topAccountsIterator.next();
            AccountEffectiveBalanceDto accountJson = accountBalance(account, false, blockchain.getHeight());
            putAccountNameInfo(accountJson, account.getId(), false);
            dto.topHolders.add(accountJson);
        }
        return dto;
    }
    private AccountEffectiveBalanceDto accountBalance(Account account, boolean includeEffectiveBalance, int height) {
        AccountEffectiveBalanceDto json = new AccountEffectiveBalanceDto();
        if (account != null) {
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

    public BlockchainStatusDto getBlockchainStatus() {
        BlockchainStatusDto dto = new BlockchainStatusDto();
        dto.application = Constants.APPLICATION;
        dto.version = Constants.VERSION.toString();
        dto.time = timeService.getEpochTime();

        if (blockchain.isInitialized()) {
            Block lastBlock = blockchain.getLastBlock();
            dto.lastBlock = lastBlock.getStringId();
            dto.cumulativeDifficulty = lastBlock.getCumulativeDifficulty().toString();
            dto.numberOfBlocks = lastBlock.getHeight() + 1;
            Block shardInitialBlock = blockchain.getShardInitialBlock();
            dto.shardInitialBlock = shardInitialBlock.getId();
            dto.lastShardHeight = shardInitialBlock.getHeight();
        }

        Peer lastBlockchainFeeder = blockchainProcessor.getLastBlockchainFeeder();
        dto.lastBlockchainFeeder = lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress();
        dto.lastBlockchainFeederHeight = blockchainProcessor.getLastBlockchainFeederHeight();
        dto.isScanning = blockchainProcessor.isScanning();
        dto.isDownloading = blockchainProcessor.isDownloading();
        dto.maxRollback = propertiesHolder.MAX_ROLLBACK();
        dto.currentMinRollbackHeight = blockchainProcessor.getMinRollbackHeight();

        dto.maxPrunableLifetime = blockchainConfig.getMaxPrunableLifetime();
        dto.includeExpiredPrunable = propertiesHolder.INCLUDE_EXPIRED_PRUNABLE();
        dto.correctInvalidFees = propertiesHolder.correctInvalidFees();
        dto.ledgerTrimKeep = AccountLedger.trimKeep;
        dto.chainId = blockchainConfig.getChain().getChainId();
        dto.chainName = blockchainConfig.getChain().getName();
        dto.chainDescription = blockchainConfig.getChain().getDescription();
        dto.blockTime = blockchainConfig.getCurrentConfig().getBlockTime();
        dto.adaptiveForging = blockchainConfig.getCurrentConfig().isAdaptiveForgingEnabled();
        dto.adaptiveBlockTime = blockchainConfig.getCurrentConfig().getAdaptiveBlockTime();
        dto.consensus = blockchainConfig.getCurrentConfig().getConsensusType().toString();
        dto.maxBlockPayloadLength = blockchainConfig.getCurrentConfig().getMaxPayloadLength();
        dto.initialBaseTarget = Long.toUnsignedString(blockchainConfig.getCurrentConfig().getInitialBaseTarget());
        dto.coinSymbol = blockchainConfig.getCoinSymbol();
        dto.accountPrefix = blockchainConfig.getAccountPrefix();
        dto.projectName = blockchainConfig.getProjectName();
        peersService.getServices().forEach(service -> dto.services.add(service.name()));

        if (APIProxy.isActivated()) {
            String servingPeer = APIProxy.getInstance().getMainPeerAnnouncedAddress();
            dto.apiProxy = true;
            dto.apiProxyPeer = servingPeer;
        }
        dto.isLightClient = propertiesHolder.isLightClient();
        dto.maxAPIRecords = API.maxRecords;
        dto.blockchainState = peersService.getMyBlockchainState() != null ?
            peersService.getMyBlockchainState().toString() : "unknown";

        return dto;
    }
}
