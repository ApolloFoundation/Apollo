/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.dto.ApolloX509Info;
import com.apollocurrency.aplwallet.api.dto.GeneratorInfo;
import com.apollocurrency.aplwallet.api.dto.info.ApiTagDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainConstantsDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStateDto;
import com.apollocurrency.aplwallet.api.dto.info.BlockchainStatusDto;
import com.apollocurrency.aplwallet.api.dto.info.NameCodeTypeDto;
import com.apollocurrency.aplwallet.api.dto.info.SubTypeDto;
import com.apollocurrency.aplwallet.api.dto.info.TimeDto;
import com.apollocurrency.aplwallet.api.dto.info.TotalSupplyDto;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.VoteWeighting;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipantState;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APIProxy;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AdminPasswordVerifier;
import com.apollocurrency.aplwallet.apl.core.monetary.HoldingType;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.PollService;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.service.state.TradeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLeaseService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountLedgerService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetTransferService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyTransferService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.MonetaryCurrencyMintingService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeRequestService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.AskOrderService;
import com.apollocurrency.aplwallet.apl.core.service.state.qualifier.BidOrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsAskOrderPlacement;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import com.apollocurrency.aplwallet.apl.crypto.HashFunction;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionType.lookupShufflingService;

/**
 * @author alukin@gmail.com
 */
@Slf4j
@Singleton
public class ServerInfoService {
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final PropertiesHolder propertiesHolder;
    private final BlockchainProcessor blockchainProcessor;
    private final PeersService peersService;
    private final TimeService timeService;
    private final AccountService accountService;
    private final AccountLedgerService accountLedgerService;
    private final AccountPublicKeyService accountPublicKeyService;
    private final DGSService dgsService;
    private final PrunableMessageService prunableMessageService;
    private final TaggedDataService taggedDataService;
    private final AccountLeaseService accountLeaseService;
    private final AdminPasswordVerifier apw;
    private final UPnP upnp;
    private final AliasService aliasService;
    private final OrderService<AskOrder, ColoredCoinsAskOrderPlacement> askOrderService;
    private final OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService;
    private final TradeService tradeService;
    private final AccountControlPhasingService accountControlPhasingService;
    private final AssetService assetService;
    private final PollService pollService;
    private final AssetTransferService assetTransferService;
    private final CurrencyExchangeOfferFacade currencyExchangeOfferFacade;
    private final ExchangeService exchangeService;
    private final ExchangeRequestService exchangeRequestService;
    private final CurrencyTransferService currencyTransferService;
    private final CurrencyService currencyService;
    private final ShufflingService shufflingService;

    @Inject
    public ServerInfoService(BlockchainConfig blockchainConfig, Blockchain blockchain,
                             PropertiesHolder propertiesHolder,
                             BlockchainProcessor blockchainProcessor,
                             PeersService peersService, TimeService timeService,
                             AccountService accountService, AccountLedgerService accountLedgerService,
                             AccountPublicKeyService accountPublicKeyService,
                             DGSService dgsService,
                             PrunableMessageService prunableMessageService,
                             TaggedDataService taggedDataService,
                             AccountLeaseService accountLeaseService,
                             AdminPasswordVerifier apw,
                             UPnP upnp,
                             AliasService aliasService,
                             @AskOrderService OrderService<AskOrder, ColoredCoinsAskOrderPlacement> askOrderService,
                             @BidOrderService OrderService<BidOrder, ColoredCoinsBidOrderPlacement> bidOrderService,
                             TradeService tradeService,
                             AccountControlPhasingService accountControlPhasingService,
                             AssetService assetService,
                             PollService pollService,
                             AssetTransferService assetTransferService,
                             CurrencyExchangeOfferFacade currencyExchangeOfferFacade,
                             ExchangeService exchangeService,
                             ExchangeRequestService exchangeRequestService,
                             CurrencyTransferService currencyTransferService,
                             CurrencyService currencyService,
                             ShufflingService shufflingService
    ) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor, "blockchainProcessor is NULL");
        this.peersService = Objects.requireNonNull(peersService, "peersService is NULL");
        this.timeService = Objects.requireNonNull(timeService, "timeService is NULL");
        this.accountService = Objects.requireNonNull(accountService, "accountService is NULL");
        this.accountLedgerService = Objects.requireNonNull(accountLedgerService, "accountLedgerService is NULL");
        this.accountPublicKeyService = Objects.requireNonNull(accountPublicKeyService, "accountPublicKeyService is NULL");
        this.dgsService = Objects.requireNonNull(dgsService, "dgsService is NULL");
        this.prunableMessageService = Objects.requireNonNull(prunableMessageService, "prunableMessageService is NULL");
        this.taggedDataService = Objects.requireNonNull(taggedDataService, "taggedDataService is NULL");
        this.accountLeaseService = Objects.requireNonNull(accountLeaseService, "accountLeaseService is NULL");
        this.apw = Objects.requireNonNull(apw, "adminPasswordVerifier is NULL");
        this.upnp = Objects.requireNonNull(upnp, "upnp is NULL");
        this.aliasService = Objects.requireNonNull(aliasService, "aliasService is NULL");
        this.askOrderService = Objects.requireNonNull(askOrderService, "askOrderService is NULL");
        this.bidOrderService = Objects.requireNonNull(bidOrderService, "bidOrderService is NULL");
        this.tradeService = Objects.requireNonNull(tradeService, "tradeService is NULL");
        this.accountControlPhasingService = Objects.requireNonNull(accountControlPhasingService, "accountControlPhasingService is NULL");
        this.assetService = Objects.requireNonNull(assetService, "assetService is NULL");
        this.pollService = Objects.requireNonNull(pollService, "pollService is NULL");
        this.assetTransferService = Objects.requireNonNull(assetTransferService, "assetTransferService is NULL");
        this.currencyExchangeOfferFacade = Objects.requireNonNull(currencyExchangeOfferFacade, "currencyExchangeOfferFacade is NULL");
        this.exchangeService = Objects.requireNonNull(exchangeService, "exchangeService is NULL");
        this.exchangeRequestService = Objects.requireNonNull(exchangeRequestService, "exchangeRequestService is NULL");
        this.currencyTransferService = Objects.requireNonNull(currencyTransferService, "currencyTransferService is NULL");
        this.currencyService = Objects.requireNonNull( currencyService, "currencyService is NULL");
        this.shufflingService = Objects.requireNonNull( shufflingService, "shufflingService is NULL");
    }

    public ApolloX509Info getX509Info() {
        ApolloX509Info res = new ApolloX509Info();
        res.id = "No ID yet available";
        return res;
    }

    public List<GeneratorInfo> getActiveForgers(boolean showBallances) {
        List<GeneratorInfo> res = new ArrayList<>();
        List<Generator> forgers = Generator.getSortedForgers();
        for (Generator g : forgers) {
            GeneratorInfo gi = new GeneratorInfo();
            gi.setAccount(g.getAccountId());
            gi.setDeadline(g.getDeadline());
            gi.setHitTime(g.getHitTime());
            if (showBallances) {
                gi.setEffectiveBalanceAPL(g.getEffectiveBalance().longValue());
            } else {
                gi.setEffectiveBalanceAPL(0L);
            }
            res.add(gi);
        }
        return res;
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
            dto.currencyTypes.add(new NameCodeTypeDto(currencyType.toString(), currencyType.getCode()));
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
        for (HashFunction hashFunction : MonetaryCurrencyMintingService.acceptedHashFunctions) {
            dto.mintingHashAlgorithms.add(new NameCodeTypeDto(hashFunction.toString(), hashFunction.getId()));
        }
        for (PeerState peerState : PeerState.values()) {
            dto.peerStates.add(new NameCodeTypeDto(peerState.toString(), peerState.ordinal()));
        }
        dto.maxTaggedDataDataLength = Constants.MAX_TAGGED_DATA_DATA_LENGTH;

        for (HoldingType holdingType : HoldingType.values()) {
            dto.holdingTypes.add(new NameCodeTypeDto(holdingType.toString(), holdingType.getCode()));
        }
        for (ShufflingStage stage : ShufflingStage.values()) {
            dto.shufflingStages.add(new NameCodeTypeDto(stage.toString(), stage.getCode()));
        }
        for (ShufflingParticipantState state : ShufflingParticipantState.values()) {
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

    public BlockchainStateDto getBlockchainState(Boolean includeCounts) {
        BlockchainStatusDto blockchainStatusDto = this.getBlockchainStatus(); // get state data first
        BlockchainStateDto dto = new BlockchainStateDto(blockchainStatusDto);
        if (includeCounts != null && includeCounts) {
            dto.numberOfTransactions = blockchain.getTransactionCount();
            dto.numberOfAccounts = accountPublicKeyService.getCount();
            dto.numberOfAssets = assetService.getCount();
            int askCount = askOrderService.getCount();
            int bidCount = bidOrderService.getCount();
            dto.numberOfOrders = askCount + bidCount;
            dto.numberOfAskOrders = askCount;
            dto.numberOfBidOrders = bidCount;
            dto.numberOfTrades = tradeService.getCount();
            dto.numberOfTransfers = assetTransferService.getCount();
            dto.numberOfCurrencies = currencyService.getCount();
            dto.numberOfOffers = currencyExchangeOfferFacade.getCurrencyBuyOfferService().getCount();
            dto.numberOfExchangeRequests = exchangeRequestService.getCount();
            dto.numberOfExchanges = exchangeService.getCount();
            dto.numberOfCurrencyTransfers = currencyTransferService.getCount();
            dto.numberOfAliases = aliasService.getCount();
            dto.numberOfGoods = dgsService.getGoodsCount();
            dto.numberOfPurchases = dgsService.getPurchaseCount();
            dto.numberOfTags = dgsService.getTagsCount();
            dto.numberOfPolls = pollService.getCount();
            dto.numberOfVotes = pollService.getPollVoteCount();
            dto.numberOfPrunableMessages = prunableMessageService.getCount();
            dto.numberOfTaggedData = taggedDataService.getTaggedDataCount();
            dto.numberOfDataTags = taggedDataService.getDataTagCount();
            dto.numberOfAccountLeases = accountLeaseService.getAccountLeaseCount();
            dto.numberOfActiveAccountLeases = accountService.getActiveLeaseCount();
            dto.numberOfShufflings = shufflingService.getShufflingCount();
            dto.numberOfActiveShufflings = lookupShufflingService().getShufflingActiveCount();
            dto.numberOfPhasingOnlyAccounts = accountControlPhasingService.getCount();
        }
        dto.numberOfPeers = peersService.getAllPeers().size();
        dto.numberOfActivePeers = peersService.getActivePeers().size();
        dto.numberOfUnlockedAccounts = Generator.getAllGenerators().size();
        dto.availableProcessors = Runtime.getRuntime().availableProcessors();
        dto.maxMemory = Runtime.getRuntime().maxMemory();
        dto.totalMemory = Runtime.getRuntime().totalMemory();
        dto.freeMemory = Runtime.getRuntime().freeMemory();
        dto.peerPort = PeersService.myPort;
        dto.isOffline = propertiesHolder.isOffline();
        dto.needsAdminPassword = !apw.isDisabledAdminPassword();
        dto.customLoginWarning = propertiesHolder.customLoginWarning();
        InetAddress externalAddress = upnp.getExternalAddress();
        if (externalAddress != null) {
            dto.upnpExternalAddress = externalAddress.getHostAddress();
        }
        return dto;
    }

    public TimeDto getTime() {
        TimeDto dto = new TimeDto();
        dto.time = timeService.getEpochTime();
        return dto;
    }

    public TotalSupplyDto getTotalSupply() {
        TotalSupplyDto dto = new TotalSupplyDto();
        dto.totalAmount = accountService.getTotalSupply();
        return dto;
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> map = new HashMap<>();
        propertiesHolder.getProperties().forEach((k, v) -> {
                if (k.equals("apl.adminPassword")
                    || k.equals("apl.dbPassword") || k.equals("apl.dbUsername")
                    || k.equals("apl.testDbPassword") || k.equals("apl.testDbUsername")) {
                    map.put(k.toString(), "***"); // password only
                } else {
                    map.put(k.toString(), v);
                }
            }
        );
        return map;
    }

}
