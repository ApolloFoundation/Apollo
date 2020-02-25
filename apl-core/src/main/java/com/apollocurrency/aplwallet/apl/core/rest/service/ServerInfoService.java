/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.ApolloX509Info;
import com.apollocurrency.aplwallet.api.dto.GeneratorInfo;
import com.apollocurrency.aplwallet.api.dto.info.AccountEffectiveBalanceDto;
import com.apollocurrency.aplwallet.api.dto.info.AccountsCountDto;
import com.apollocurrency.aplwallet.api.dto.info.ApiTagDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainConstantsDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStateDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.api.dto.info.NameCodeTypeDto;
import com.apollocurrency.aplwallet.api.dto.info.SubTypeDto;
import com.apollocurrency.aplwallet.api.dto.info.TimeDto;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.Convert2;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.app.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.app.mint.CurrencyMinting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.monetary.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
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
//    private DatabaseManager databaseManager;
//    private AccountTable accountTable;
    private BlockchainConfig blockchainConfig;
    private Blockchain blockchain;
    private PropertiesHolder propertiesHolder;
    private BlockchainProcessor blockchainProcessor;
    private PeersService peersService;
    private TimeService timeService;
    private AccountService accountService;
    private AccountLedgerService accountLedgerService;

    @Inject
    public ServerInfoService(//DatabaseManager databaseManager, AccountTable accTable,
                             BlockchainConfig blockchainConfig, Blockchain blockchain,
                             PropertiesHolder propertiesHolder,
                             BlockchainProcessor blockchainProcessor,
                             PeersService peersService, TimeService timeService,
                             AccountService accountService, AccountLedgerService accountLedgerService) {
//        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
//        this.accountTable = Objects.requireNonNull(accTable, "accTable is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder,"propertiesHolder is NULL");
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor,"blockchainProcessor is NULL");
        this.peersService = Objects.requireNonNull(peersService,"peersService is NULL");
        this.timeService = Objects.requireNonNull(timeService,"timeService is NULL");
        this.accountService = Objects.requireNonNull(accountService,"accountService is NULL");
        this.accountLedgerService = Objects.requireNonNull(accountLedgerService,"accountLedgerService is NULL");
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
        long totalSupply = accountService.getTotalSupply();
        long totalAccounts = accountService.getTotalNumberOfAccounts();
        long totalAmountOnTopAccounts = accountService.getTotalAmountOnTopAccounts(numberOfAccounts);
        List<Account> topHoldersIterator = accountService.getTopHolders(numberOfAccounts);
        composeAccountCountDto(dto, topHoldersIterator, totalAmountOnTopAccounts, totalSupply, totalAccounts, numberOfAccounts);
        return dto;
    }

    private AccountsCountDto composeAccountCountDto(AccountsCountDto dto, List<Account> topAccountsIterator,
                                                    long totalAmountOnTopAccounts, long totalSupply,
                                                    long totalAccounts, int numberOfAccounts) {
        dto.totalSupply = totalSupply;
        dto.totalNumberOfAccounts = totalAccounts;
        dto.numberOfTopAccounts = numberOfAccounts;
        dto.totalAmountOnTopAccounts = totalAmountOnTopAccounts;
        while (topAccountsIterator.iterator().hasNext()) {
            Account account = topAccountsIterator.iterator().next();
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
                json.effectiveBalanceAPL = accountService.getEffectiveBalanceAPL(account , height, false);
                json.guaranteedBalanceATM = accountService.getGuaranteedBalanceATM(account,
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
        dto.ledgerTrimKeep = accountLedgerService.getTrimKeep();
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

    public BlockchainConstantsDto getBlockchainConstants() {
        BlockchainConstantsDto dto = new BlockchainConstantsDto();
        if (blockchain.isInitialized()) {
            dto.genesisBlockId = Long.toUnsignedString(blockchain.getBlockIdAtHeight(0));
        }
        dto.genesisAccountId = Long.toUnsignedString(GenesisImporter.CREATOR_ID);
        dto.epochBeginning = GenesisImporter.EPOCH_BEGINNING;
        dto.maxArbitraryMessageLength = Constants.MAX_ARBITRARY_MESSAGE_LENGTH;
        dto.maxPrunableMessageLength = Constants.MAX_PRUNABLE_MESSAGE_LENGTH;

        dto.coinSymbol = blockchainConfig.getCoinSymbol();
        dto.accountPrefix = blockchainConfig.getAccountPrefix();
        dto.projectName = blockchainConfig.getProjectName();

        dto.maxImportSecretFileLength = propertiesHolder.getIntProperty("apl.maxKeyStoreFileSize");
        dto.gasLimitEth = Constants.GAS_LIMIT_ETHER_TX;
        dto.gasLimitERC20 = Constants.GAS_LIMIT_FOR_ERC20;

        outer:
        for (int type = 0; ; type++) {
            List<SubTypeDto> subtypeList = new ArrayList<>(10);
            for (int subtype = 0; ; subtype++) {
                TransactionType transactionType;
                try {
                    transactionType = TransactionType.findTransactionType((byte) type, (byte) subtype);
                } catch (IllegalArgumentException ignore) {
                    continue;
                }
                if (transactionType == null) {
                    if (subtype == 0) {
                        break outer;
                    } else {
                        break;
                    }
                }
                SubTypeDto subtypeJSON = new SubTypeDto();
                subtypeJSON.name = transactionType.getName();
                subtypeJSON.canHaveRecipient = transactionType.canHaveRecipient();
                subtypeJSON.mustHaveRecipient = transactionType.mustHaveRecipient();
                subtypeJSON.isPhasingSafe = transactionType.isPhasingSafe();
                subtypeJSON.isPhasable = transactionType.isPhasable();
                subtypeJSON.type = type;
                subtypeJSON.subtype = subtype;
                subtypeList.add(subtypeJSON);
                dto.transactionSubTypes.add(subtypeJSON);
            }
            dto.transactionTypes.add(subtypeList);
        }
        // several types generated to JSON
        for (CurrencyType currencyType : CurrencyType.values()) {
            dto.currencyTypes.add(new NameCodeTypeDto(currencyType.toString(), currencyType.getCode() ));
        }
        for (VoteWeighting.VotingModel votingModel : VoteWeighting.VotingModel.values()) {
            dto.votingModels.add(new NameCodeTypeDto(votingModel.toString(), votingModel.getCode()));
        }
        for (VoteWeighting.MinBalanceModel minBalanceModel : VoteWeighting.MinBalanceModel.values()) {
            dto.minBalanceModels.add(new NameCodeTypeDto(minBalanceModel.toString(), minBalanceModel.getCode()));
        }
        for (HashFunction hashFunction : HashFunction.values()) {
            dto.hashAlgorithms.add(new NameCodeTypeDto(hashFunction.toString(), hashFunction.getId()));
        }
        for (HashFunction hashFunction : PhasingPollService.HASH_FUNCTIONS) {
            dto.phasingHashAlgorithms.add(new NameCodeTypeDto(hashFunction.toString(), hashFunction.getId()));
        }
        dto.maxPhasingDuration = Constants.MAX_PHASING_DURATION;
        for (HashFunction hashFunction : CurrencyMinting.acceptedHashFunctions) {
            dto.mintingHashAlgorithms.add(new NameCodeTypeDto(hashFunction.toString(), hashFunction.getId()));
        }
        for (PeerState peerState : PeerState.values()) {
            dto.peerStates.add(new NameCodeTypeDto(peerState.toString(), peerState.ordinal()));
        }
        dto.maxTaggedDataDataLength = Constants.MAX_TAGGED_DATA_DATA_LENGTH;

        for (HoldingType holdingType : HoldingType.values()) {
            dto.holdingTypes.add(new NameCodeTypeDto(holdingType.toString(), holdingType.getCode()));
        }
        for (Shuffling.Stage stage : Shuffling.Stage.values()) {
            dto.shufflingStages.add(new NameCodeTypeDto(stage.toString(), stage.getCode()));
        }
        for (ShufflingParticipant.State state : ShufflingParticipant.State.values()) {
            dto.shufflingParticipantStates.add(new NameCodeTypeDto(state.toString(), state.getCode()));
        }
        for (APITag apiTag : APITag.values()) {
            ApiTagDto tagJSON = new ApiTagDto();
            tagJSON.tagName = apiTag.name();
            tagJSON.name = apiTag.getDisplayName();
            tagJSON.enabled = !API.disabledAPITags.contains(apiTag);
            dto.apiTags.add(tagJSON);
        }
        return dto;
    }

    public BlockchainStateDto getBlockchainState(boolean includeCounts, boolean isValidAdminPassword) {
        BlockchainStatusDto blockchainStatusDto = this.getBlockchainStatus(); // get state data first
        BlockchainStateDto dto = new BlockchainStateDto(blockchainStatusDto);
        return dto;
    }

    public TimeDto getTime() {
        TimeDto dto = new TimeDto();
        dto.time = timeService.getEpochTime();
        return dto;
    }

}
