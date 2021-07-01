/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainScanException;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.AccountLedgerEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ScanValidate;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.app.runnable.GetMoreBlocksThread;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.blockchain.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.blockchain.BlockchainProcessorState;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ScanDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.util.db.DbTransactionHelper;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ScanEntity;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.files.shards.ShardsDownloadService;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetNextBlocksResponseParser;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.GeneratorService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableRestorationService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardImporter;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.apollocurrency.aplwallet.apl.util.task.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.environment.se.WeldContainer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class BlockchainProcessorImpl implements BlockchainProcessor {

    private static final String BACKGROUND_SERVICE_NAME = "BlockchainService";
    private static final Comparator<Transaction> finishingTransactionsComparator = Comparator
        .comparingInt(Transaction::getHeight)
        .thenComparingInt(Transaction::getIndex)
        .thenComparingLong(Transaction::getId);
    private static final Comparator<UnconfirmedTransaction> transactionArrivalComparator = Comparator
        .comparingLong(UnconfirmedTransaction::getArrivalTimestamp)
        .thenComparingInt(UnconfirmedTransaction::getHeight)
        .thenComparingLong(UnconfirmedTransaction::getId);

    private final PropertiesHolder propertiesHolder;
    private final BlockchainConfig blockchainConfig;
    private final DexService dexService;
    private final DatabaseManager databaseManager;
    private final ExecutorService networkService;

    private final javax.enterprise.event.Event<Block> blockEvent;
    private final javax.enterprise.event.Event<AccountLedgerEventType> ledgerEvent;
    private final javax.enterprise.event.Event<List<Transaction>> txEvent;
    private final javax.enterprise.event.Event<BlockchainConfig> blockchainEvent;
    private final GlobalSync globalSync;
    private final DerivedTablesRegistry dbTables;
    private final ReferencedTransactionService referencedTransactionService;
    private final PhasingPollService phasingPollService;
    private final TransactionValidator transactionValidator;
    private final TransactionApplier transactionApplier;
    private final TransactionBuilderFactory transactionBuilderFactory;
    private final TrimService trimService;
    private final ShardImporter shardImporter;
    private final AplAppStatus aplAppStatus;
    private final BlockApplier blockApplier;
    private final ShardsDownloadService shardDownloader;
    private final ShardDao shardDao;
    private final PrunableLoadingService prunableService;
    private final TransactionJsonSerializer transactionJsonSerializer;
    private final PeersService peersService;
    private final BlockchainConfigUpdater blockchainConfigUpdater;
    private final FullTextSearchService fullTextSearchProvider;
    private final TaskDispatchManager taskDispatchManager;
    private final Blockchain blockchain;
    private final BlockEntityRowMapper blockEntityRowMapper;
    private final TransactionProcessor transactionProcessor;
    private final TimeService timeService;
    private final PrunableRestorationService prunableRestorationService;
    private final BlockchainProcessorState blockchainProcessorState;
    private final AccountControlPhasingService accountControlPhasingService; // lazy initialization only !
    private final BlockValidator validator;
    private final AccountService accountService;
    private final GeneratorService generatorService;
    private final BlockParser blockParser;
    private final GetNextBlocksResponseParser getNextBlocksResponseParser;
    private final BlockSerializer blockSerializer;
    private final ConsensusManager consensusManager;
    private final MemPool memPool;
    private Map<String, String> fullTextSearchIndexedTables;
    private TxBContext txBContext;
    private final ScanDao scanDao;

    /**
     * Three blocks are used for internal calculations on assigning previous block
     */
    private Block[] threeLatestBlocksArray = new Block[3];

    @Inject
    public BlockchainProcessorImpl(PropertiesHolder propertiesHolder, BlockchainConfig blockchainConfig,
                                   BlockValidator validator, Event<Block> blockEvent, Event<AccountLedgerEventType> ledgerEvent,
                                   GlobalSync globalSync, DerivedTablesRegistry dbTables,
                                   ReferencedTransactionService referencedTransactionService, PhasingPollService phasingPollService,
                                   TransactionValidator transactionValidator,
                                   TransactionApplier transactionApplier,
                                   TrimService trimService, DatabaseManager databaseManager, DexService dexService,
                                   BlockApplier blockApplier, AplAppStatus aplAppStatus,
                                   ShardsDownloadService shardDownloader,
                                   ShardImporter importer,
                                   TaskDispatchManager taskDispatchManager, Event<List<Transaction>> txEvent,
                                   Event<BlockchainConfig> blockchainEvent,
                                   TransactionBuilderFactory transactionBuilderFactory, ShardDao shardDao,
                                   PrunableLoadingService prunableService, TransactionJsonSerializer transactionJsonSerializer, TimeService timeService,
                                   AccountService accountService,
                                   AccountControlPhasingService accountControlPhasingService,
                                   BlockchainConfigUpdater blockchainConfigUpdater,
                                   PrunableRestorationService prunableRestorationService,
                                   Blockchain blockchain,
                                   BlockEntityRowMapper blockEntityRowMapper,
                                   PeersService peersService,
                                   TransactionProcessor transactionProcessor,
                                   FullTextSearchService fullTextSearchProvider,
                                   GeneratorService generatorService,
                                   BlockParser blockParser,
                                   GetNextBlocksResponseParser getNextBlocksResponseParser,
                                   BlockSerializer blockSerializer,
                                   ConsensusManager consensusManager,
                                   MemPool memPool,
                                   @Named(value = "fullTextTables") Map<String, String> fullTextSearchIndexedTables,
                                    ScanDao scanDao) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.blockchainConfig = blockchainConfig;
        this.validator = validator;
        this.blockEvent = blockEvent;
        this.ledgerEvent = ledgerEvent;
        this.globalSync = globalSync;
        this.memPool = memPool;
        this.dbTables = dbTables;
        this.trimService = trimService;
        this.phasingPollService = phasingPollService;
        this.transactionValidator = transactionValidator;
        this.transactionApplier = transactionApplier;
        this.referencedTransactionService = referencedTransactionService;
        this.databaseManager = databaseManager;
        this.dexService = dexService;
        this.transactionBuilderFactory = transactionBuilderFactory;
        this.prunableService = prunableService;
        this.transactionJsonSerializer = transactionJsonSerializer;
        this.scanDao = scanDao;
        this.networkService = getNetworkServiceExecutor();
        this.blockApplier = blockApplier;
        this.aplAppStatus = aplAppStatus;
        this.shardDownloader = shardDownloader;
        this.shardImporter = importer;
        this.taskDispatchManager = taskDispatchManager;
        this.txEvent = txEvent;
        this.blockchainEvent = blockchainEvent;
        this.shardDao = shardDao;
        this.timeService = timeService;
        this.accountService = accountService;
        this.accountControlPhasingService = accountControlPhasingService;
        this.blockchainConfigUpdater = blockchainConfigUpdater;
        this.prunableRestorationService = prunableRestorationService;

        this.blockchain = blockchain;
        this.blockEntityRowMapper = blockEntityRowMapper;
        this.peersService = peersService;
        this.transactionProcessor = transactionProcessor;
        this.fullTextSearchProvider = fullTextSearchProvider;
        this.blockchainProcessorState = new BlockchainProcessorState();
        this.generatorService = generatorService;
        this.blockParser = blockParser;
        this.getNextBlocksResponseParser = getNextBlocksResponseParser;
        this.blockSerializer = blockSerializer;
        this.consensusManager = consensusManager;
        this.fullTextSearchIndexedTables = fullTextSearchIndexedTables;

        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());

        configureBackgroundTasks();
    }

    private ExecutorService getNetworkServiceExecutor() {
        final NamedThreadFactory threadFactory = new NamedThreadFactory("BlockchainProcessor:networkService");
        ExecutorService executorService;
        if (propertiesHolder.getBooleanProperty("apl.limitHardwareResources", false)) {
            executorService = new ThreadPoolExecutor(
                propertiesHolder.getIntProperty("apl.networkServiceCorePoolSize"),
                propertiesHolder.getIntProperty("apl.networkServiceMaximumPoolSize"),
                60L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
        } else {
            executorService = Executors.newCachedThreadPool(threadFactory);
        }
        return executorService;
    }


    private void configureBackgroundTasks() {
        TaskDispatcher dispatcher = taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME);

        Task blockChainInitTask = Task.builder()
            .name("BlockchainInit")
            .task(() -> {
                checkResumeDownloadDecideShardImport(); // continue blockchain automatically or try import genesis / shard data
                if (propertiesHolder.getBooleanProperty("apl.forceScan")) {
                    scan(new ScanEntity(propertiesHolder.getBooleanProperty("apl.forceValidate"), 0, false ));
                } else {
                    ScanEntity scanEntity = scanDao.get();
                    if (scanEntity != null && scanEntity.isRescan()) {
                        scan(scanEntity);
                    } else {
                        if (blockchain.getShardInitialBlock() == null) { // prevent NPE on empty node
                            trimService.resetTrim();
                        }
                    }
                }
            }).build();

        dispatcher.invokeInit(blockChainInitTask);


        if (!propertiesHolder.isLightClient() && !propertiesHolder.isOffline()) {
            Task moreBlocksTask = Task.builder()
                .name("GetMoreBlocks")
                .delay(250)
                .initialDelay(250)
                .task(new GetMoreBlocksThread(this, blockchainProcessorState,
                    blockchainConfig, blockchain, peersService,
                    globalSync, timeService, prunableRestorationService,
                    networkService, propertiesHolder, transactionProcessor, getNextBlocksResponseParser,
                    blockSerializer)
                )
                .build();

            dispatcher.schedule(moreBlocksTask);
        }}

    @Override
    public Peer getLastBlockchainFeeder() {
        return blockchainProcessorState.getLastBlockchainFeeder();
    }

    @Override
    public int getLastBlockchainFeederHeight() {
        return blockchainProcessorState.getLastBlockchainFeederHeight();
    }

    @Override
    public boolean isTrimming(){
        return trimService.isTrimming();
    }

    @Override
    public boolean isScanning() {
        return blockchainProcessorState.isScanning();
    }

    @Override
    public int getInitialScanHeight() {
        return blockchainProcessorState.getInitialScanHeight();
    }

    @Override
    public boolean isDownloading() {
        return blockchainProcessorState.isDownloading();
    }

    @Override
    public boolean isProcessingBlock() {
        return blockchainProcessorState.isProcessingBlock();
    }

    @Override
    public int getMinRollbackHeight() {
        int minRollBackHeight = trimService.getLastTrimHeight() > 0 ? trimService.getLastTrimHeight()
            : Math.max(blockchain.getHeight() - propertiesHolder.MAX_ROLLBACK(), 0);
        log.trace("minRollbackHeight  = {}", minRollBackHeight);

        return minRollBackHeight;
    }

    @Override
    public void processPeerBlock(JSONObject request) throws AplException {
        globalSync.updateLock();
        try {
            Block lastBlock = blockchain.getLastBlock();
            long peerBlockPreviousBlockId = Convert.parseUnsignedLong((String) request.get("previousBlock"));
            log.trace("Timeout: peerBlock{},ourBlock{}", request.get("timeout"), lastBlock.getTimeout());
            log.trace("Timestamp: peerBlock{},ourBlock{}", request.get("timestamp"), lastBlock.getTimestamp());
            log.trace("PrevId: peerBlock{},ourBlock{}", peerBlockPreviousBlockId, lastBlock.getPreviousBlockId());
            // peer block is the next block in our blockchain
            long baseTarget = blockchainConfig.getCurrentConfig().getInitialBaseTarget();
            if (peerBlockPreviousBlockId == lastBlock.getId()) {
                log.debug("push peer last block");
                Block block = blockParser.parseBlock(request, baseTarget);
                pushBlock(block);
            } else if (peerBlockPreviousBlockId == lastBlock.getPreviousBlockId()) { //peer block is a candidate to replace our last block
                Block block = blockParser.parseBlock(request, baseTarget);
                //try to replace our last block by peer block only when real block time of the peer block is less than a real timestamp of our block,
                // or when block time is equal, but peer's block has better timeout
                int peerBlockTime = block.getTimestamp() - block.getTimeout();
                int ourBlockTime = lastBlock.getTimestamp() - lastBlock.getTimeout();
                if (peerBlockTime < ourBlockTime || (peerBlockTime == ourBlockTime && block.getTimeout() < lastBlock.getTimeout())) {
                    log.debug("Need to replace block");
                    Block lb = blockchain.getLastBlock();
                    if (lastBlock.getId() != lb.getId()) {
                        log.debug("Block changed: expected: id {} height: {} generator: {}, got id {}, height {}, generator {} ", lastBlock.getId(),
                            lastBlock.getHeight(), Convert2.rsAccount(lastBlock.getGeneratorId()), lb.getId(), lb.getHeight(),
                            Convert2.rsAccount(lb.getGeneratorId()));
                        return; // blockchain changed, ignore the block
                    }
                    Block previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                    lastBlock = popOffToCommonBlock(previousBlock).get(0);
                    try {
                        pushBlock(block);
                        log.debug("Pushed better peer block: id {} height: {} generator: {}",
                            block.getId(),
                            block.getHeight(),
                            Convert2.rsAccount(block.getGeneratorId()));
                        transactionProcessor.processLater(lastBlock.getTransactions());
                        log.debug("Last block " + lastBlock.getStringId() + " was replaced by " + block.getStringId());
                    } catch (BlockNotAcceptedException e) {
                        log.debug("Replacement block failed to be accepted, pushing back our last block");
                        pushBlock(lastBlock);
                        transactionProcessor.processLater(block.getTransactions());
                    }
                }
            }// else ignore the block
        } finally {
            globalSync.updateUnlock();
        }
    }

    @Override
    public List<Block> popOffTo(int height) {
        if (height <= 0) {
            log.debug("Do 'popOff' to ZERO by supplied height='{}'<=0", height);
//            fullReset(); // now doesn't work as expected on full/shard db
            return popOffToCommonBlock(blockchain.getBlockAtHeight(0)); // can trigger rescan !
        } else if (height < blockchain.getHeight()) {
            log.debug("Do 'popOff' to height='{}'", height);
            return popOffToCommonBlock(blockchain.getBlockAtHeight(height));
        }
        return Collections.emptyList();
    }

    @Override
    public void fullReset() {
        log.debug("FULL reset...");
        globalSync.writeLock();
        try {
            suspendBlockchainDownloading();
            try {
                TransactionalDataSource dataSource = databaseManager.getDataSource();
                dataSource.begin();
                try {
                    blockchain.deleteAll();
                    dbTables.getDerivedTables().forEach(DerivedTableInterface::truncate);
                    ((DatabaseManagerImpl) databaseManager).closeAllShardDataSources();
                    trimService.resetTrim();
                    DirProvider dirProvider = RuntimeEnvironment.getInstance().getDirProvider();
                    Path dataExportDir = dirProvider.getDataExportDir();
                    FileUtils.clearDirectorySilently(dataExportDir);
                    FileUtils.deleteFilesByPattern(dirProvider.getDbDir(), new String[]{".zip", DbProperties.DB_EXTENSION_WITH_DOT}, new String[]{"-shard-"});
                    dataSource.commit(false);
                    blockchainConfigUpdater.rollback(0);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                    dataSource.rollback(false);
                } finally {
                    dataSource.commit();
                }
                checkResumeDownloadDecideShardImport();// continue blockchain automatically or try import genesis / shard data
            } finally {
                resumeBlockchainDownloading();
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public List<Transaction> getExpectedTransactions(Filter<Transaction> filter) {
        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        List<Transaction> result = new ArrayList<>();
        globalSync.readLock();
        try {
            List<Transaction> phasedTransactions = phasingPollService.getFinishingTransactions(blockchain.getHeight() + 1);
            for (Transaction phasedTransaction : phasedTransactions) {
                try {
                    transactionValidator.validateFully(phasedTransaction);
                    // prefetch data for duplicate validation
                    Account senderAccount = accountService.getAccount(phasedTransaction.getSenderId());
                    Set<AccountControlType> senderAccountControls = senderAccount.getControls();
                    AccountControlPhasing accountControlPhasing = accountControlPhasingService.get(phasedTransaction.getSenderId());
                    if (!phasedTransaction.attachmentIsDuplicate(duplicates, false, senderAccountControls, accountControlPhasing)
                        && filter.test(phasedTransaction)) {
                        result.add(phasedTransaction);
                    }
                } catch (AplTransactionValidationException ignore) {
                }
            }

            selectUnconfirmedTransactions(duplicates, blockchain.getLastBlock(), -1, Integer.MAX_VALUE).forEach(
                unconfirmedTransaction -> {
                    Transaction transaction = unconfirmedTransaction.getTransactionImpl();
                    if (transaction.getPhasing() == null && filter.test(transaction)) {
                        result.add(transaction);
                    }
                }
            );
        } finally {
            globalSync.readUnlock();
        }
        return result;
    }

    public void shutdown() {
        log.info("BlchProcImpl shutdown started...");
        try {
            //blockchainEvent.select(BlockchainEventType.literal(BlockchainEventType.SHUTDOWN)).fire(blockchainConfig);//TODO: Is this event necessary at this point?
            suspendBlockchainDownloading();
            Tasks.shutdownExecutor("BlockchainProcessorNetworkService", networkService, 5);
            log.info("BlchProcImpl shutdown finished");
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void addBlock(Block block) {
        blockchain.saveBlock(block);
        blockchain.setLastBlock(block);
    }

    private void checkResumeDownloadDecideShardImport() {
        Block lastBlock = blockchain.getLastBlock(); // blockchain should be initialized independently
        if (lastBlock != null) {
            // continue blockchain automatically
            log.info("Genesis block already in database");
            blockchain.deleteBlocksFromHeight(lastBlock.getHeight() + 1);
            popOffToCommonBlock(lastBlock);
            log.info("Last block height: " + lastBlock.getHeight());
            resumeBlockchainDownloading(); // turn ON blockchain downloading
            return;
        }
        // NEW START-UP logic, try import genesis OR start downloading shard zip data
        suspendBlockchainDownloading(); // turn off automatic blockchain downloading
        long peerConnectionWaitDelayMS = 10000L;
        try {
            log.warn("----!!!>>> NODE IS WAITING FOR '{}' milliseconds about 'shard/no_shard decision' " +
                "and proceeding with necessary data later by receiving NO_SHARD / SHARD_PRESENT event....", peerConnectionWaitDelayMS);
            // try make delay before PeersService are up and running
            Thread.sleep(peerConnectionWaitDelayMS); // milli-seconds to wait for PeersService initialization
            // ignore result, because async event is expected/received by 'ShardDownloadPresenceObserver' component
            FileDownloadDecision downloadDecision = shardDownloader.tryDownloadLastGoodShard();
            log.debug("NO_SHARD/SHARD_PRESENT decision was = '{}'", downloadDecision);
        } catch (InterruptedException e) {
            log.error("main BlockchainProcessorImpl thread was interrupted, EXITING...");
            System.exit(-1);
        }
    }

    /**
     * Pushes the new last block into blockchain fully validating all the transactions
     * This method uses the full blockchain lock {@link GlobalSync#writeLock()}
     * @param block block to add into blockchain
     * @throws com.apollocurrency.aplwallet.apl.util.db.DbTransactionHelper.DbTransactionExecutionException which wraps
     * a real exception cause, this type of exceptions is rare and usually should not be handled, since something wrong
     * with the running environment: database deadlocks, timeout locking table, too many connections and so on
     * @throws com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor.BlockNotAcceptedException in case of the block validation failure
     */
    @Override
    public void pushBlock(final Block block) throws BlockNotAcceptedException {
        int curTime = timeService.getEpochTime();
        log.trace("push new block, prev_id = '{}', cutTime={}", block.getPreviousBlockId(), curTime);
        long startTime = System.currentTimeMillis();
        globalSync.writeLock();
        long lockAquireTime = System.currentTimeMillis() - startTime;
        Block previousLastBlock;
        previousLastBlock = blockchain.getLastBlock();
        try {
            try {
                DbTransactionHelper.executeInTransaction(databaseManager.getDataSource(), () -> {

                    validator.validate(block, previousLastBlock, curTime);

                    long nextHitTime = generatorService.getNextHitTime(previousLastBlock.getId(), curTime);
                    if (nextHitTime > 0 && block.getTimestamp() > nextHitTime + 1) {
                        String msg = "Rejecting block " + block.getStringId() + " at height " + previousLastBlock.getHeight()
                            + " block timestamp " + block.getTimestamp() + " next hit time " + nextHitTime
                            + " current time " + curTime;
                        log.debug(msg);
                        generatorService.setDelay(-propertiesHolder.FORGING_SPEEDUP());
                        throw new BlockOutOfOrderException(msg, blockSerializer.getJSONObject(block));
                    }

                    Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
                    List<Transaction> validPhasedTransactions = new ArrayList<>();
                    List<Transaction> invalidPhasedTransactions = new ArrayList<>();
                    validatePhasedTransactions(block, previousLastBlock, validPhasedTransactions, invalidPhasedTransactions, duplicates);
                    validateTransactions(block, previousLastBlock, curTime, duplicates, previousLastBlock.getHeight() >= Constants.LAST_CHECKSUM_BLOCK);

                    HeightConfig config = blockchainConfig.getCurrentConfig();
                    Shard lastShard = shardDao.getLastShard();
                    Block shardInitialBlock = blockchain.getShardInitialBlock();
                    int currentHeight = previousLastBlock.getHeight();
                    // put three latest blocks into array TODO: YL optimize to fetch three blocks later
                    fillInBlockArray(previousLastBlock, lastShard, currentHeight);
                    consensusManager.setPrevious(block, threeLatestBlocksArray, config, lastShard, shardInitialBlock.getHeight());
                    block.assignTransactionsIndex(); // IMPORTANT step !!!
                    log.trace("fire block on = {}, id = '{}', '{}'", block.getHeight(), block.getId(), BlockEventType.BEFORE_BLOCK_ACCEPT.name());
                    blockEvent.select(literal(BlockEventType.BEFORE_BLOCK_ACCEPT)).fire(block);
                    addBlock(block);

                    accept(block, validPhasedTransactions, invalidPhasedTransactions, duplicates);

                    blockchain.commit(block);
                    log.trace("committed block on = {}, id = '{}'", block.getHeight(), block.getId());
                });
            } catch (DbTransactionHelper.DbTransactionExecutionException e) {
                log.error("PushBlock, error:", e);
                try {
                    popOffToCommonBlock(previousLastBlock); // do in current transaction
                } catch (Exception ex) {
                    log.error("Unable to rollback db changes or do pop off for block height " + block.getHeight() + " id " + block.getId(), ex);
                } finally { // set blockchain last block to point on previous correct block in any case to restore operability and avoid BlockNotFoundException
                    // Need to do so even in case of popOff failure or transaction rollback fatal error (SYS table lock, connection closed, etc)
                    blockchain.setLastBlock(previousLastBlock);
                }
                Throwable realEx = e.getCause();
                if (realEx instanceof BlockNotAcceptedException) {
                    throw (BlockNotAcceptedException) realEx;
                }
                throw e;
            }
            log.trace("fire block on = {}, id = '{}', '{}'", block.getHeight(), block.getId(), BlockEventType.AFTER_BLOCK_ACCEPT.name());
            blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(block);
        } finally {
            globalSync.writeUnlock();
        }
        if (block.getTimestamp() >= curTime - 600) {
            log.debug("From pushBlock, Send block to peers: height: {} id: {} generator:{}", block.getHeight(), Long.toUnsignedString(block.getId()),
                Convert2.rsAccount(block.getGeneratorId()));
            try {
                peersService.sendToSomePeers(block);
            } catch (RejectedExecutionException ignored) {

            }
        }
        log.trace("fire block on = {}, id = '{}', '{}'", block.getHeight(), Long.toUnsignedString(block.getId()), BlockEventType.BLOCK_PUSHED.name());
        blockEvent.select(literal(BlockEventType.BLOCK_PUSHED)).fire(block); // send sync event to TrimObserver component
        blockEvent.select(literal(BlockEventType.BLOCK_PUSHED)).fireAsync(block); // send async event to other components
        log.debug("Push block at height {} tx cnt: {} took {} ms (lock acquiring: {} ms)",
            block.getHeight(), block.getTransactions().size(), System.currentTimeMillis() - startTime, lockAquireTime);
    }

    //TODO implement cache for the recent blocks/transactions and all shard data
    private void fillInBlockArray(Block previousLastBlock, Shard lastShard, int currentHeight) {
        threeLatestBlocksArray[0] = previousLastBlock;
        if (lastShard == null) {
            if (currentHeight >= 1) {
                threeLatestBlocksArray[1] = blockchain.getBlockAtHeight(currentHeight - 1);
            }
            if (currentHeight >= 2) {
                threeLatestBlocksArray[2] = blockchain.getBlockAtHeight(currentHeight - 2);
            }
        } else {
            if ((currentHeight - 1) >= lastShard.getShardHeight()) {
                threeLatestBlocksArray[1] = blockchain.getBlockAtHeight(currentHeight - 1);
            }
            if ((currentHeight - 2) >= lastShard.getShardHeight()) {
                threeLatestBlocksArray[2] = blockchain.getBlockAtHeight(currentHeight - 2);
            }
        }
    }

    private AnnotationLiteral<BlockEvent> literal(BlockEventType blockEventType) {
        return new BlockEventBinding() {
            @Override
            public BlockEventType value() {
                return blockEventType;
            }
        };
    }

    private void validatePhasedTransactions(Block currentBlock, Block prevBlock, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions,
                                            Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        int height = prevBlock.getHeight();

        List<Transaction> transactions = new ArrayList<>(phasingPollService.getFinishingTransactions(prevBlock.getHeight() + 1));

        transactions.addAll(phasingPollService.getFinishingTransactionsByTime(prevBlock.getTimestamp(), currentBlock.getTimestamp()));

        for (Transaction phasedTransaction : transactions) {
            if (phasingPollService.getResult(phasedTransaction.getId()) != null) {
                continue;
            }
            try {
                transactionValidator.validateFully(phasedTransaction);
                // prefetch data for duplicate validation
                Account senderAccount = accountService.getAccount(phasedTransaction.getSenderId());
                Set<AccountControlType> senderAccountControls = senderAccount.getControls();
                AccountControlPhasing accountControlPhasing = accountControlPhasingService.get(phasedTransaction.getSenderId());
                if (!phasedTransaction.attachmentIsDuplicate(duplicates, false, senderAccountControls, accountControlPhasing)) {
                    validPhasedTransactions.add(phasedTransaction);
                } else {
                    log.debug("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " is duplicate, will not apply");
                    invalidPhasedTransactions.add(phasedTransaction);
                }
            } catch (AplTransactionValidationException e) {
                log.debug("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " no longer passes validation: "
                    + e.getMessage() + ", will not apply");
                invalidPhasedTransactions.add(phasedTransaction);
            }
        }
    }

    private void validateTransactions(Block block, Block previousLastBlock, int curTime, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates,
                                      boolean fullValidation) throws BlockNotAcceptedException {
        long payloadLength = 0;
        long calculatedTotalAmount = 0;
        long calculatedTotalFee = 0;
        MessageDigest digest = Crypto.sha256();
        boolean hasPrunedTransactions = false;
        for (Transaction transaction : block.getTransactions()) {
            if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
                throw new BlockOutOfOrderException("Invalid transaction timestamp: " + transaction.getTimestamp()
                    + ", current time is " + curTime, blockSerializer.getJSONObject(block));
            }
            if (!transactionValidator.verifySignature(transaction)){
                throw new TransactionNotAcceptedException("Transaction signature verification failed at height " + previousLastBlock.getHeight(), transaction, blockSerializer.getJSONObject(block));
            }
            if (fullValidation) {
                if (transaction.getTimestamp() > block.getTimestamp() + Constants.MAX_TIMEDRIFT
                    || transaction.getExpiration() < block.getTimestamp()) {
                    throw new TransactionNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                        + ", current time is " + curTime + ", block timestamp is " + block.getTimestamp(),
                        transaction, blockSerializer.getJSONObject(block));
                }
                if (blockchain.hasTransaction(transaction.getId(), previousLastBlock.getHeight())) {
                    throw new TransactionNotAcceptedException(
                        "Transaction is already in the blockchain",
                        transaction, blockSerializer.getJSONObject(block));
                }
                if (transaction.referencedTransactionFullHash() != null && !referencedTransactionService.hasAllReferencedTransactions(transaction, previousLastBlock.getHeight() + 1)) {
                    throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
                        + transaction.getReferencedTransactionFullHash(),
                        transaction, blockSerializer.getJSONObject(block));
                }
                if (transaction.getId() == 0L) {
                    throw new TransactionNotAcceptedException(
                        "Invalid transaction id 0", transaction, blockSerializer.getJSONObject(block));
                }
                try {
                    transactionValidator.validateFully(transaction);
                } catch (AplUnacceptableTransactionValidationException e) {
                    throw new TransactionNotAcceptedException(e,
                        transaction, blockSerializer.getJSONObject(block));
                } catch (AplAcceptableTransactionValidationException e) {
                    transaction.fail(e.getMessage());
                }
            }
            // prefetch data for duplicate validation
            Account senderAccount = accountService.getAccount(transaction.getSenderId());
            Set<AccountControlType> senderAccountControls = senderAccount.getControls();
            AccountControlPhasing accountControlPhasing = accountControlPhasingService.get(transaction.getSenderId());
            if (transaction.attachmentIsDuplicate(duplicates, true, senderAccountControls, accountControlPhasing)) {
                throw new TransactionNotAcceptedException(
                    "Transaction is a duplicate", transaction, blockSerializer.getJSONObject(block));
            }
            if (!hasPrunedTransactions) {
                for (Appendix appendage : transaction.getAppendages()) {
                    if ((appendage instanceof Prunable) && !((Prunable) appendage).hasPrunableData()) {
                        hasPrunedTransactions = true;
                        break;
                    }
                }
            }
            calculatedTotalAmount += transaction.getAmountATM();
            calculatedTotalFee += transaction.getFeeATM();
            Result result = getTxByteArrayResult(transaction);
            digest.update(result.array());
            payloadLength += TransactionUtils.calculateFullSize(transaction, result.size());
        }
        if (calculatedTotalAmount != block.getTotalAmountATM() || calculatedTotalFee != block.getTotalFeeATM()) {
            throw new BlockNotAcceptedException(
                "Total amount or fee don't match transaction totals", blockSerializer.getJSONObject(block));
        }
        if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
            throw new BlockNotAcceptedException(
                "Payload hash doesn't match", blockSerializer.getJSONObject(block));
        }
        if (hasPrunedTransactions ? payloadLength > block.getPayloadLength() : payloadLength != block.getPayloadLength()) {
            throw new BlockNotAcceptedException(
                "Transaction payload length " + payloadLength + " does not match block payload length "
                    + block.getPayloadLength(), blockSerializer.getJSONObject(block));
        }
    }

    private Result getTxByteArrayResult(Transaction transaction) {
        Result result = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(transaction, result);
        return result;
    }

    private void accept(Block block, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions,
                        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) throws TransactionNotAcceptedException {
        long start = System.currentTimeMillis();
        try {
            log.debug(":accept: Accepting block: {} height: {}", block.getId(), block.getHeight());
            blockchainProcessorState.setProcessingBlock(true);
            for (Transaction transaction : block.getTransactions()) {
                if (!transactionApplier.applyUnconfirmed(transaction)) {
                    throw new TransactionNotAcceptedException(
                        "Double spending", transaction, blockSerializer.getJSONObject(block));
                }
            }
            log.trace(":accept: apply(block) block: {} height: {}", block.getId(), block.getHeight());
            blockEvent.select(literal(BlockEventType.BEFORE_BLOCK_APPLY)).fire(block);
            blockApplier.apply(block);
            log.trace(":accept: validPhasedTransaction ctx count={}", validPhasedTransactions.size());
            validPhasedTransactions.forEach(phasingPollService::countVotesAndRelease);
            log.trace(":accept: invalidPhasedTransaction ctx count={}", invalidPhasedTransactions.size());
            invalidPhasedTransactions.forEach(phasingPollService::reject);
            int fromTimestamp = timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime();
            log.trace(":accept: load transactions fromTimestamp={}", fromTimestamp);
            for (Transaction transaction : block.getTransactions()) {
                try {
                    transactionApplier.apply(transaction);
                    if (transaction.getTimestamp() > fromTimestamp) {
                        for (AbstractAppendix appendage : transaction.getAppendages()) {
                            prunableService.loadPrunable(transaction, appendage, true);
                            if ((appendage instanceof Prunable) &&
                                !((Prunable) appendage).hasPrunableData()) {
                                // TODO: YL check correct work with prunables
                                Set<Long> prunableTransactions = prunableRestorationService.getPrunableTransactions();
                                synchronized (prunableTransactions) {
                                    prunableTransactions.add(transaction.getId());
                                }
                                blockchainProcessorState.setLastRestoreTime(0);
                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    log.error(e.toString(), e);
                    throw new BlockchainProcessor.TransactionNotAcceptedException(
                        e, transaction, blockSerializer.getJSONObject(block));
                }
            }
            SortedSet<Transaction> possiblyApprovedTransactions = new TreeSet<>(finishingTransactionsComparator);
            log.trace(":accept: validate all block transactions");
            block.getTransactions().forEach(transaction -> {
                phasingPollService.getLinkedPhasedTransactions(transaction.getFullHash()).forEach(phasedTransaction -> {
                    if ((phasedTransaction.getPhasing().getFinishHeight() > block.getHeight()
                        || phasedTransaction.getPhasing().getClass() == PhasingAppendixV2.class
                        && ((PhasingAppendixV2) phasedTransaction.getPhasing()).getFinishTime() > block.getTimestamp()
                    )
                        && phasingPollService.getResult(phasedTransaction.getId()) == null) {
                        possiblyApprovedTransactions.add(phasedTransaction);
                    }
                });
                if (transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PHASING_VOTE_CASTING && !transaction.attachmentIsPhased()) {
                    MessagingPhasingVoteCasting voteCasting = (MessagingPhasingVoteCasting) transaction.getAttachment();
                    voteCasting.getTransactionFullHashes().forEach(hash -> {
                        PhasingPoll phasingPoll = phasingPollService.getPoll(Convert.transactionFullHashToId(hash));
                        if (phasingPoll.allowEarlyFinish()
                            && (phasingPoll.getFinishHeight() > block.getHeight()
                            || phasingPoll.getFinishTime() > block.getTimestamp())
                            && phasingPollService.getResult(phasingPoll.getId()) == null) {
                            possiblyApprovedTransactions.add(blockchain.getTransaction(phasingPoll.getId()));
                        }
                    });
                }
            });
            log.trace(":accept: validate Valid phasing transactions");
            validPhasedTransactions.forEach(phasedTransaction -> {
                if (phasedTransaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.PHASING_VOTE_CASTING) {
                    PhasingPollResult result = phasingPollService.getResult(phasedTransaction.getId());
                    if (result != null && result.isApproved()) {
                        MessagingPhasingVoteCasting phasingVoteCasting = (MessagingPhasingVoteCasting) phasedTransaction.getAttachment();
                        phasingVoteCasting.getTransactionFullHashes().forEach(hash -> {
                            PhasingPoll phasingPoll = phasingPollService.getPoll(Convert.transactionFullHashToId(hash));
                            if (phasingPoll.allowEarlyFinish()
                                && (phasingPoll.getFinishHeight() > block.getHeight()
                                || phasingPoll.getFinishTime() > block.getTimestamp())
                                && phasingPollService.getResult(phasingPoll.getId()) == null) {
                                possiblyApprovedTransactions.add(blockchain.getTransaction(phasingPoll.getId()));
                            }
                        });
                    }
                }
            });
            log.trace(":accept: validate Approved transactions");
            possiblyApprovedTransactions.forEach(transaction -> {
                // checked before
                //                if (phasingPollService.getResult(transaction.getId()) == null) {
                try {
                    transactionValidator.validateFully(transaction);
                    phasingPollService.tryCountVotes(transaction, duplicates);
                } catch (AplTransactionValidationException e) {
                    log.debug("At height " + block.getHeight() + " phased transaction " + transaction.getStringId()
                        + " no longer passes validation: " + e.getMessage() + ", cannot finish early");
                }
//                }
            });
            log.trace(":accept: dex service block.");

            try {
                dexService.closeOverdueOrders(block.getTimestamp());

                if (blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3() != null && block.getHeight() > blockchainConfig.getDexExpiredContractWithFinishedPhasingHeightAndStep3()) {
                    dexService.closeExpiredContractsStep1_2_3(block.getTimestamp());
                } else {
                    dexService.closeExpiredContractsStep1_2(block.getTimestamp());
                }


                if (blockchainConfig.getDexPendingOrdersReopeningHeight() != null && block.getHeight() >= blockchainConfig.getDexPendingOrdersReopeningHeight()) {
                    dexService.reopenPendingOrders(block.getHeight(), block.getTimestamp());
                }
            } catch (AplException.ExecutiveProcessException e) {
                log.error(e.toString(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
            log.trace(":accept: fire AFTER_BLOCK_APPLY.");
            blockEvent.select(literal(BlockEventType.AFTER_BLOCK_APPLY)).fire(block);
            log.trace(":accept: fire for All block transactions ADDED_CONFIRMED_TRANSACTIONS.");
            if (block.getTransactions().size() > 0) {
                txEvent.select(TxEventType.literal(TxEventType.ADDED_CONFIRMED_TRANSACTIONS)).fire(
                    block.getTransactions());
            }
            log.trace(":accept: Fire event COMMIT_ENTRIES");
            ledgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.COMMIT_ENTRIES)).fire(AccountLedgerEventType.COMMIT_ENTRIES);
            log.trace(":accept: that's it.");
        } finally {
            blockchainProcessorState.setProcessingBlock(false);
            log.trace("Fire event CLEAR_ENTRIES");
            ledgerEvent.select(AccountLedgerEventBinding.literal(AccountLedgerEventType.CLEAR_ENTRIES)).fire(AccountLedgerEventType.CLEAR_ENTRIES);
            log.trace("Accepting block DONE: {} height: {} processing time ms: {}", block.getId(), block.getHeight(), System.currentTimeMillis() - start);
        }
    }

    public List<Block> popOffToCommonBlock(Block commonBlock) {
        if (commonBlock == null) {
            // that case is possible on sharded node without full blockchain parts
            log.error("Sorry, it's NOT POSSIBLE make popOff to specified height, returning...");
            return Collections.emptyList();
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        globalSync.writeLock();
        try {
            if (!dataSource.isInTransaction()) {
                try {
                    dataSource.begin();
                    return popOffToInTransaction(commonBlock, dataSource);
                } finally {
                    dataSource.commit();
                }
            } else {
                return popOffToInTransaction(commonBlock, dataSource);
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    public List<Block> popOffToInTransaction(Block commonBlock, TransactionalDataSource dataSource) {
        return popOffToInTransaction(commonBlock, dataSource, 0);
    }

    public List<Block> popOffToInTransaction(Block commonBlock, TransactionalDataSource dataSource, int attempt) {
        int minRollbackHeight = getMinRollbackHeight();
        int commonBlockHeight = commonBlock.getHeight();
        log.debug(">> popOffToInTransaction() to commonBlockHeight = {}, minRollbackHeight={}", commonBlockHeight, minRollbackHeight);
        if (commonBlockHeight < minRollbackHeight) {
            log.info("Rollback to commonBlockHeight " + commonBlockHeight + " not supported, will do a full rescan");

            // usually = 0 on full node (or on sharded node without any shard yet)
            // > 0 on sharded node with one or more shards
            int shardInitialHeight = blockchain.getShardInitialBlock().getHeight();
            if (commonBlockHeight < shardInitialHeight) {
                // when we have a shard on node, we can't scan below 'latest' snapshot block in main db
                log.warn("Popping the blocks off that before the last shard block is not supported (commonBlockHeight={} < shardInitialHeight={})",
                    commonBlockHeight, shardInitialHeight);
            } else {
                // check shard conditions...
                HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
                boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
                boolean shardingEnabled = currentConfig.isShardingEnabled();
                log.debug("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
                if (shardInitialHeight != 0 && shardingEnabled && !isShardingOff) {
                    // sharding is enabled and turned ON
                    log.warn("DO NOT do 'popOffWithRescan' to commonBlockHeight(+1) = {} / shardInitialHeight={}, it NEEDs refactoring...",
                        commonBlockHeight + 1, shardInitialHeight);
//                    popOffWithRescan(commonBlockHeight + 1); // YL: needs more investigation and scan refactoring for shard case
                } else {
                    // sharding is DISABLED and turned OFF, FULL DB mode
                    log.warn("DO 'popOffWithRescan' to commonBlockHeight(+1) = {}...", commonBlockHeight + 1);
                    popOffWithRescan(commonBlockHeight + 1); // 'full node' can go to full rescan here
                }
            }
            return Collections.emptyList();
        }
        if (!blockchain.hasBlock(commonBlock.getId())) {
            log.debug("Block " + commonBlock.getStringId() + " not found in blockchain, nothing to pop off");
            return Collections.emptyList();
        }
        List<Block> poppedOffBlocks = new ArrayList<>();
        try {
            Block block = blockchain.getLastBlock();
            log.debug("ROLLBACK from block " + block.getStringId() + " at height " + block.getHeight()
                + " to " + commonBlock.getStringId() + " at " + commonBlockHeight);
            while (block.getId() != commonBlock.getId() && block.getHeight() > 0) {
                poppedOffBlocks.add(block);
                block = popLastBlock();
            }
            long rollbackStartTime = System.currentTimeMillis();
            log.debug("Start rollback for tables=[{}]", dbTables.getDerivedTables().size());
            if (log.isTraceEnabled()) {
                log.trace("popOffToInTransaction rollback: {}", dbTables.toString());
            }
            for (DerivedTableInterface table : dbTables.getDerivedTables()) {
                long start = System.currentTimeMillis();
                table.rollback(commonBlockHeight);
                if (log.isTraceEnabled()) {
                    log.trace("rollback for table={} to commonBlockHeight={} in {} ms", table.getName(),
                        commonBlockHeight, System.currentTimeMillis() - start);
                }
            }
            log.debug("Total rollback time: {} ms", System.currentTimeMillis() - rollbackStartTime);
            dataSource.commit(false); // should happen definitely otherwise
            log.debug("<< popOffToInTransaction() blocks=[{}] at commonBlockHeight={}", poppedOffBlocks.size(), commonBlockHeight);
        } catch (RuntimeException e) {
            log.error("Error popping off to " + commonBlockHeight, e);
            dataSource.rollback(false);
            if (blockchain != null) { //prevent NPE on shutdown
                Block lastBlock = blockchain.findLastBlock();
                if (lastBlock == null) {
                    log.error("Error popping off, lastBlock is NULL.", e);
                } else {
                    if (!isDeadlockEx(e) && lastBlock.equals(commonBlock)) {
                        if (++attempt > 2) { // shutdown the node only when more than two unsuccessful popOffs are done for the same block
                            log.error("FATAL ERROR: failed popping off to the current last block: " + commonBlock
                                + ". Cannot guarantee consistency of the blockchain after that, will not try to popOff to the same lastBlock." +
                                " Shutdown the node", e);
                            System.exit(-1);
                        }
                    }
                    blockchain.setLastBlock(lastBlock);
                    popOffToInTransaction(lastBlock, dataSource, attempt);
                }
            }
            throw e;
        }
        return poppedOffBlocks;
    }

    private boolean isDeadlockEx(RuntimeException e) {
        return StringUtils.isNotBlank(e.getMessage()) && e.getMessage().contains("Deadlock found when trying to get lock");
    }

    private Block popLastBlock() {
        Block block = blockchain.getLastBlock();
        if (block.getHeight() == 0) {
            throw new RuntimeException("Cannot pop off genesis block");
        }
        Block previousBlock = blockchain.deleteBlocksFrom(block.getId()); // load block data
        blockchain.setLastBlock(previousBlock);
        blockEvent.select(literal(BlockEventType.BLOCK_POPPED)).fire(block);
        return previousBlock;
    }

    private void popOffWithRescan(int height) {
        log.debug(">> popOffWithRescan to height = " + height);
        globalSync.writeLock();
        try {
            int scanHeight = 0;
            int shardInitialHeight = blockchain.getShardInitialBlock().getHeight();
            if (shardInitialHeight > 0) {
                scanHeight = Math.max(height, shardInitialHeight);
            }
            log.debug("Set scanHeight={}, shard's initialBlockHeight={}, currentHeight={}", scanHeight, shardInitialHeight, height);
            ScanEntity scanEntity = new ScanEntity(false, scanHeight, false);
            try {
                scanDao.saveOrUpdate(scanEntity);
                long blockIdAtHeight = blockchain.getBlockIdAtHeight(height);
                log.debug("popOffWithRescan blockIdAtHeight={}", blockIdAtHeight);
                Block lastBLock = blockchain.deleteBlocksFrom(blockIdAtHeight);
                log.debug("popOffWithRescan lastBLock={}", lastBLock);
                for (DerivedTableInterface derivedTable : dbTables.getDerivedTables()) {
                    // rollback not scan safe, 'prunable tables' only
                    if (!derivedTable.isScanSafe()) {
                        long start = System.currentTimeMillis();
                        derivedTable.rollback(height);
                        log.debug("rollback on height={} table={} in {} ms", height,
                            derivedTable.getName(), System.currentTimeMillis() - start);
                    }
                }
                log.debug("popOffWithRescan set to lastBLock={}", lastBLock);
                blockchain.setLastBlock(lastBLock);
                blockchainConfigUpdater.rollback(lastBLock.getHeight());
                log.debug("Blockchain config updated, lastBlockId={} at height={}", lastBLock.getId(), lastBLock.getHeight());
            } catch (Exception e) {
                // just for logging possible hidden error
                log.error("popOffWithRescan Error", e);
            } finally {
                try {
                    scan(scanEntity);
                } catch (BlockchainScanException e) {
                    log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + e.toString(), e);
                }
            }
        } finally {
            globalSync.writeUnlock();
        }
        log.debug("<< popOffWithRescan to height = " + height);
    }

    public SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(
        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, Block previousBlock, int blockTimestamp, int limit) {

        List<UnconfirmedTransaction> orderedUnconfirmedTransactions = new ArrayList<>();
        CollectionUtil.forEach(memPool.getAllStream()
            .filter(transaction -> referencedTransactionService.hasAllReferencedTransactions(
                transaction.getTransactionImpl(), previousBlock.getHeight() + 1)), orderedUnconfirmedTransactions::add);
        SortedSet<UnconfirmedTransaction> sortedTransactions = new TreeSet<>(transactionArrivalComparator);
        int maxPayloadLength = blockchainConfig.getCurrentConfig().getMaxPayloadLength();
        List<UnconfirmedTransaction> removedTxs = new ArrayList<>();
        List<UnconfirmedTransaction> appliedUnconfirmedTxs = new ArrayList<>();
        DbTransactionHelper.executeInTransaction(databaseManager.getDataSource(), () -> {
            int payloadLength = 0;
            RuntimeException selectTxEx = null;
            try {
                txSelectLoop:
                while (payloadLength <= maxPayloadLength && sortedTransactions.size() <= blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()) {
                    int prevNumberOfNewTransactions = sortedTransactions.size();
                    for (UnconfirmedTransaction unconfirmedTransaction : orderedUnconfirmedTransactions) {
                        if (memPool.isRemoved(unconfirmedTransaction)) {
                            continue;
                        }
                        int transactionLength = unconfirmedTransaction.getFullSize();
                        if (sortedTransactions.contains(unconfirmedTransaction) || payloadLength + transactionLength > maxPayloadLength) {
                            continue;
                        }
                        if (blockTimestamp > 0 && (unconfirmedTransaction.getTimestamp() > blockTimestamp + Constants.MAX_TIMEDRIFT
                            || unconfirmedTransaction.getExpiration() < blockTimestamp)) {
                            continue;
                        }
                        try {
                            transactionValidator.validateSufficiently(unconfirmedTransaction.getTransactionImpl());
                        } catch (AplUnacceptableTransactionValidationException e) {
                            continue;
                        }
                        if (!transactionApplier.applyUnconfirmed(unconfirmedTransaction.getTransactionImpl())) { // persist tx changes and validate against updated state
                            removedTxs.add(unconfirmedTransaction); // remove incorrect transaction and forget about it for 10 minutes
                            continue;
                        } else {
                            appliedUnconfirmedTxs.add(unconfirmedTransaction);
                        }
                        // prefetch data for duplicate validation
                        Account senderAccount = accountService.getAccount(unconfirmedTransaction.getTransactionImpl().getSenderId());
                        Set<AccountControlType> senderAccountControls = senderAccount.getControls();
                        AccountControlPhasing accountControlPhasing = accountControlPhasingService.get(
                            unconfirmedTransaction.getTransactionImpl().getSenderId());
                        if (unconfirmedTransaction.getTransactionImpl().attachmentIsDuplicate(
                            duplicates, true, senderAccountControls, accountControlPhasing)) {
                            continue;
                        }
                        sortedTransactions.add(unconfirmedTransaction);
                        if (sortedTransactions.size() == limit) {
                            break txSelectLoop;
                        }
                        payloadLength += transactionLength;
                    }
                    if (sortedTransactions.size() == prevNumberOfNewTransactions) {
                        break;
                    }
                }
            } catch (RuntimeException e) {
                selectTxEx = e;
            }
            revertUnconfirmedChanges(appliedUnconfirmedTxs, selectTxEx);
            if (selectTxEx != null) {
                throw selectTxEx;
            }
        });
        removedTxs.forEach(transactionProcessor::removeUnconfirmedTransaction); // commit db transaction for each tx
        return sortedTransactions;
    }

    public SortedSet<UnconfirmedTransaction> getUnconfirmedTransactions(Block previousBlock, int blockTimestamp, int limit) {
        Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        List<Transaction> phasedTransactions = phasingPollService.getFinishingTransactions(blockchain.getHeight() + 1);
        phasedTransactions.addAll(phasingPollService.getFinishingTransactionsByTime(previousBlock.getTimestamp(), blockTimestamp));
        for (Transaction phasedTransaction : phasedTransactions) {
            try {
                transactionValidator.validateFully(phasedTransaction);
                // prefetch data for duplicate validation
                Account senderAccount = accountService.getAccount(phasedTransaction.getSenderId());
                Set<AccountControlType> senderAccountControls = senderAccount.getControls();
                AccountControlPhasing accountControlPhasing = accountControlPhasingService.get(phasedTransaction.getSenderId());
                phasedTransaction.attachmentIsDuplicate(
                    duplicates, false, senderAccountControls, accountControlPhasing); // pre-populate duplicates map
            } catch (AplTransactionValidationException ignore) {
            }
        }
        SortedSet<UnconfirmedTransaction> sortedTransactions = selectUnconfirmedTransactions(duplicates, previousBlock, blockTimestamp, limit);
        return sortedTransactions;
    }

    public void generateBlock(byte[] keySeed, int blockTimestamp, int timeout, int blockVersion) throws BlockNotAcceptedException, MempoolStateDesyncException {
        Block previousBlock = blockchain.getLastBlock();
        SortedSet<UnconfirmedTransaction> sortedTransactions = getUnconfirmedTransactions(previousBlock, blockTimestamp, Integer.MAX_VALUE);
        log.debug("Selected txs to generate block with [{}]", sortedTransactions.stream().map(Transaction::getId).map(String::valueOf).collect(Collectors.joining(",")));
        verifyTxSufficiency(sortedTransactions, blockVersion);

        List<Transaction> blockTransactions = new ArrayList<>();
        MessageDigest digest = Crypto.sha256();
        long totalAmountATM = 0;
        long totalFeeATM = 0;
        int payloadLength = 0;
        for (UnconfirmedTransaction unconfirmedTransaction : sortedTransactions) {
            Transaction transaction = unconfirmedTransaction.getTransactionImpl();
            blockTransactions.add(transaction);
            Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);
            digest.update(signedTxBytes.array());
            totalAmountATM += transaction.getAmountATM();
            totalFeeATM += transaction.getFeeATM();
            payloadLength += TransactionUtils.calculateFullSize(transaction, signedTxBytes.size());
        }
        byte[] payloadHash = digest.digest();
        digest.update(previousBlock.getGenerationSignature());
        final byte[] publicKey = Crypto.getPublicKey(keySeed);
        byte[] generationSignature = digest.digest(publicKey);
//        blockchain.getOrLoadTransactions(previousBlock); // load transactions
        byte[] previousBlockHash = Crypto.sha256().digest(((BlockImpl) previousBlock).bytes());
        long baseTarget = blockchainConfig.getCurrentConfig().getInitialBaseTarget();
        Block block = new BlockImpl(blockVersion, blockTimestamp, previousBlock.getId(), totalAmountATM, totalFeeATM, payloadLength,
            payloadHash, publicKey, generationSignature, previousBlockHash, timeout, blockTransactions, keySeed, baseTarget);

        try {
            pushBlock(block);
            blockEvent.select(literal(BlockEventType.BLOCK_GENERATED)).fire(block);
            log.debug("Account " + Long.toUnsignedString(block.getGeneratorId()) + " generated block " + block.getStringId()
                + " at height " + block.getHeight() + " timestamp " + block.getTimestamp() + " fee " + ((float) block.getTotalFeeATM()) / blockchainConfig.getOneAPL());
        } catch (TransactionNotAcceptedException e) {
            log.debug("Generate block failed: " + e.getMessage());
            Transaction transaction = e.getTransaction();
            log.debug("Removing invalid transaction: " + transaction.getStringId());
            transactionProcessor.removeUnconfirmedTransaction(transaction);
            throw e;
        } catch (BlockNotAcceptedException e) {
            log.debug("Generate block failed: " + e.getMessage());
            throw e;
        }
    }

    private void verifyTxSufficiency(Set<UnconfirmedTransaction> txs, int blockVersion) throws MempoolStateDesyncException {
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        if (!currentConfig.isAdaptiveForgingEnabled()) {
            return;
        }
        int adaptiveTxsNumber = currentConfig.getNumberOfTransactionsInAdaptiveBlock();
        int size = txs.size();
        if (size <= adaptiveTxsNumber && blockVersion == Block.REGULAR_BLOCK_VERSION) {
            throw new MempoolStateDesyncException("Not enough transactions for regular block generation, got " + size + ", required at least > " + adaptiveTxsNumber);
        }
        if (size > adaptiveTxsNumber && blockVersion == Block.ADAPTIVE_BLOCK_VERSION) {
            throw new MempoolStateDesyncException("Expected " + adaptiveTxsNumber + " for adaptive block generation, but got " + size);
        }
        if (size <= adaptiveTxsNumber && blockVersion == Block.INSTANT_BLOCK_VERSION) {
            throw new MempoolStateDesyncException("Expected greater then " + adaptiveTxsNumber + " for instant block generation, but got " + size);
        }
    }

    @Override
    public void fullScanWithShutdown() {
        scan(new ScanEntity(true, 0, true));
    }

    @Override
    public void scan(ScanEntity scanEntity) {
        log.debug("Begin scan {}", scanEntity);
        globalSync.writeLock();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            if (!dataSource.isInTransaction()) {
                try {
                    dataSource.begin();
                    scan(scanEntity);
                    dataSource.commit();
                } catch (Exception e) {
                    dataSource.rollback();
                    throw e;
                }
                return;
            }
            int shardInitialHeight = blockchain.getShardInitialBlock().getHeight();
            boolean blockScanningStarted = scanEntity.getFromHeight() < scanEntity.getCurrentScanHeight();
            boolean scanInterrupted = blockScanningStarted || scanEntity.isPreparationDone();
            int height = blockScanningStarted ? scanEntity.getCurrentScanHeight() - 1 : scanEntity.getFromHeight();
            if (!scanInterrupted) {
                if (height < shardInitialHeight) {
                    log.warn("Scanning of blocks that before the last shard block is not supported (height={} < shardInitialHeight={})", height, shardInitialHeight);
                    finishScan(scanEntity);
                    return;
                }
                scanDao.saveOrUpdate(scanEntity);
                if (height > 0 && height < getMinRollbackHeight()) {
                    log.info("Rollback to height less than {} (min rollback height) not supported, will do a full scan", getMinRollbackHeight());
                    height = shardInitialHeight;
                }
                if (height < 0) {
                    height = shardInitialHeight;
                }
                if (!shardImporter.canImport(height)) {
                    log.info("Unable to import shard at height {}", height);
                    finishScan(scanEntity);
                    return;
                }
                if (height > blockchain.getHeight() + 1) {
                    finishScan(scanEntity);
                    String message = "Rollback height " + (height - 1) + " exceeds current blockchain height of " + blockchain.getHeight() + ", no scan needed";
                    log.info(message);
                    return;
                }
            }

            log.info("Scanning blockchain starting from height " + height + "...");
            if (scanEntity.isValidate()) {
                log.debug("Also verifying signatures and validating transactions...");
            }

            String scanTaskId = aplAppStatus.durableTaskStart("Blockchain scan", "Rollback derived tables and scan blockchain blocks and transactions from given height to extract and save derived data", true);
            blockchainProcessorState.setScanning(true);
            try (Connection con = dataSource.getConnection()) {
                blockchainProcessorState.setInitialScanHeight(blockchain.getHeight());
                if (scanEntity.getFromHeight() == shardInitialHeight) {
                    aplAppStatus.durableTaskUpdate(scanTaskId, 0.5, "Dropping all full text search indexes");
                    fullTextSearchProvider.dropAll(con);
                    aplAppStatus.durableTaskUpdate(scanTaskId, 3.5, "Full text indexes dropped successfully");
                }
                if (!scanInterrupted) {
                    if (height == shardInitialHeight) {
                        trimService.resetTrim(height);
                    }
                    Collection<DerivedTableInterface<?>> derivedTables = dbTables.getDerivedTables();
                    double percentsPerTable = getPercentsPerEvent(16.0, derivedTables.size());
                    aplAppStatus.durableTaskUpdate(scanTaskId, 4.0, "Rollback " + derivedTables.size() + " tables");
                    for (DerivedTableInterface<?> table : derivedTables) {
                        aplAppStatus.durableTaskUpdate(scanTaskId,
                            "Rollback table \'" + table.toString() + "\' to height " + height, 0.0);
                        if (table.isScanSafe()) {
                            if (height == shardInitialHeight) {
                                table.truncate();
                            } else {
                                table.rollback(height - 1);
                            }
                        }
                        aplAppStatus.durableTaskUpdate(scanTaskId, "Rollback finished for table \'" + table.toString() + "\' to height " + height, percentsPerTable);
                    }
                    dataSource.commit(false);
                    aplAppStatus.durableTaskUpdate(scanTaskId, 20.0, "Rolled back " + derivedTables.size() + " derived tables");
                }
                Block currentBlock = blockchain.getBlockAtHeight(height);
                log.debug("scan currentBlock={} at height={}", currentBlock, height);
                blockEvent.select(literal(BlockEventType.RESCAN_BEGIN)).fire(currentBlock);
                long currentBlockId = currentBlock.getId();
                blockchain.setLastBlock(currentBlock);
                if (height == shardInitialHeight && !scanInterrupted) {
                    aplAppStatus.durableTaskUpdate(scanTaskId, 20.5, "Apply genesis");
                    shardImporter.importLastShard(height);
                    aplAppStatus.durableTaskUpdate(scanTaskId, 24.5, "Genesis applied");
                }
                blockchainConfigUpdater.rollback(blockchain.getLastBlock().getHeight());
                if (scanEntity.isShutdown()) {
                    log.info("Scan will be performed at next start");
                    new Thread(() -> System.exit(0)).start();
                    return;
                }
                scanEntity.setPreparationDone(true);
                scanDao.saveOrUpdate(scanEntity);
                dataSource.commit(false);
                aplAppStatus.durableTaskUpdate(scanTaskId, 25.0, "Scanning blocks");

                int totalBlocksToScan = (blockchain.findLastBlock().getHeight() - height);
                double percentsPerThousandBlocks = getPercentsPerEvent(70.0, totalBlocksToScan / 1000);
                int blockCounter = 0;
                boolean hasMore = true;
                int initialLastBlockHeight = height;
                height--; //adjust scan height for db search
                outer:
                while (hasMore) {
                    hasMore = false;
                    List<Block> blocksToScan = blockchain.getBlocksAfter(height, 1000);
                    for (Block block : blocksToScan) {
                            try {
                                blockchain.loadBlockData(currentBlock);
                                height = block.getHeight();
                                currentBlock = block;
                                if (height > initialLastBlockHeight) { // skip first fetched block (current blockchain.lastBlock)
                                    if (currentBlock.getId() != currentBlockId || currentBlock.getHeight() > blockchain.getHeight() + 1) {
                                        throw new AplException.NotValidException("Database blocks in the wrong order!");
                                    }
                                    Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
                                    List<Transaction> validPhasedTransactions = new ArrayList<>();
                                    List<Transaction> invalidPhasedTransactions = new ArrayList<>();
                                    validatePhasedTransactions(currentBlock, blockchain.getLastBlock(), validPhasedTransactions, invalidPhasedTransactions, duplicates);
                                    if (scanEntity.isValidate()) {
                                        int curTime = timeService.getEpochTime();
                                        validator.validate(currentBlock, blockchain.getLastBlock(), curTime);
                                        byte[] blockBytes = ((BlockImpl) currentBlock).bytes();
                                        JSONObject blockJSON = blockSerializer.getJSONObject(currentBlock);
                                        long baseTarget = blockchainConfig.getCurrentConfig().getInitialBaseTarget();
                                        BlockImpl parsedBlock = blockParser.parseBlock(blockJSON, baseTarget);
                                        byte[] blockParsedBytes = parsedBlock.bytes();
                                        if (!Arrays.equals(blockBytes, blockParsedBytes)) {
                                            throw new AplException.NotValidException("Block JSON cannot be parsed back to the same block");
                                        }
                                        validateTransactions(currentBlock, blockchain.getLastBlock(), curTime, duplicates, true);
                                        for (Transaction transaction : currentBlock.getTransactions()) { // block transaction are already loaded earlier
                                            byte[] transactionBytes = getTxByteArrayResult(transaction).array();
                                            if (!Arrays.equals(transactionBytes,
                                                getTxByteArrayResult(
                                                    transactionBuilderFactory.newTransaction(transactionBytes)
                                                ).array())) {
                                                throw new AplException.NotValidException("Transaction bytes cannot be parsed back to the same transaction: "
                                                    + transactionJsonSerializer.toJson(transaction).toJSONString());
                                            }
                                            JSONObject transactionJSON = (JSONObject) JSONValue.parse(transactionJsonSerializer.toJson(transaction).toJSONString());
                                            if (!Arrays.equals(transactionBytes,
                                                getTxByteArrayResult(
                                                    transactionBuilderFactory.newTransaction(transactionJSON)
                                                ).array())) {
                                                throw new AplException.NotValidException("Transaction JSON cannot be parsed back to the same transaction: "
                                                    + transactionJsonSerializer.toJson(transaction).toJSONString());
                                            }
                                        }
                                    }
                                    blockEvent.select(literal(BlockEventType.BEFORE_BLOCK_ACCEPT)).fire(currentBlock);
                                    blockchain.setLastBlock(currentBlock);
                                    accept(currentBlock, validPhasedTransactions, invalidPhasedTransactions, duplicates);
                                    blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(currentBlock);
                                    scanEntity.setCurrentScanHeight(height + 1);
                                    scanDao.saveOrUpdate(scanEntity);
                                    dataSource.commit(false);
                                }
                                if (++blockCounter % 1000 == 0) {
                                    aplAppStatus.durableTaskUpdate(scanTaskId,
                                        "Scanned " + blockCounter + "/" + totalBlocksToScan + " blocks", percentsPerThousandBlocks);
                                }
                                currentBlockId = currentBlock.getNextBlockId();
                            } catch (AplException | RuntimeException e) {
                                dataSource.rollback(false);
                                log.debug(e.toString(), e);
                                log.debug("Applying block " + Long.toUnsignedString(currentBlockId) + " at height "
                                    + currentBlock.getHeight() + " failed during scan");
                                if (WeldContainer.getRunningContainerIds().size() > 0) {
                                    log.warn("Container is running, delete from database blocks from {}", height);
                                    Block lastBlock = blockchain.deleteBlocksFrom(currentBlockId);
                                    blockchain.setLastBlock(lastBlock);
                                    popOffToCommonBlock(lastBlock);
                                } else {
                                    log.warn("Scan was interrupted, but will be restarted at next startup");
                                    return;
                                }
                                break outer;
                            }
                            if (scanEntity.isValidate()) {
                                blockEvent.select(literal(BlockEventType.BLOCK_SCANNED), new AnnotationLiteral<ScanValidate>() {
                                }).fire(currentBlock);
                            } else {
                                blockEvent.select(literal(BlockEventType.BLOCK_SCANNED)).fire(currentBlock);
                            }
                            hasMore = true;
                        }
                    }
                aplAppStatus.durableTaskUpdate(scanTaskId, 95.0, "All blocks scanned");
                double percentsPerTableIndex = getPercentsPerEvent(4.0, dbTables.getDerivedTables().size());
                if (height == shardInitialHeight) {
                    for (DerivedTableInterface table : dbTables.getDerivedTables()) {
                        if (table instanceof SearchableTableInterface) {
                            aplAppStatus.durableTaskUpdate(scanTaskId,
                                "Create full text search index for table " + table.toString(), percentsPerTableIndex);
                            fullTextSearchProvider.createSearchIndex(con, table.getName(), table.getFullTextSearchColumns());
                        }
                    }
                }
                finishScan(scanEntity);
                blockEvent.select(literal(BlockEventType.RESCAN_END)).fire(currentBlock);
                dataSource.commit(false);
                log.info("Scan done at height " + blockchain.getHeight());
                if (height == shardInitialHeight && scanEntity.isValidate()) {
                    log.info("SUCCESSFULLY PERFORMED FULL RESCAN WITH VALIDATION");
                }
                blockchainProcessorState.setLastRestoreTime(0);
            } catch (SQLException e) {
                //if (e.getErrorCode() != 90007) { //The error with code 90007 is thrown when trying to call a JDBC method on an object that has been closed.
                try {
                    dataSource.rollback(false);
                } catch (IllegalStateException ex) {
                    log.error("Error during the Rollback caused by SQL Exception", e);
                }
                //}
                throw new BlockchainScanException(e.toString(), e);
            } finally {
                blockchainProcessorState.setScanning(false);
                aplAppStatus.durableTaskFinished(scanTaskId, false, "");
            }
        } catch (Exception e) {
            // just for logging possible hidden error
            log.error("popOffWithRescan ->scan error", e);
        } finally {
            globalSync.writeUnlock();
        }
        log.debug("End of scan {}", scanEntity);
    }

    private void finishScan(ScanEntity scanEntity) {
        scanEntity.setRescan(false);
        scanDao.saveOrUpdate(scanEntity);
    }

    private double getPercentsPerEvent(double totalPercents, int events) {
        return totalPercents / Math.max(events, 1);
    }

    @Override
    public void setGetMoreBlocks(boolean getMoreBlocks) {
        log.debug("Setting thread for block downloading into '{}'", getMoreBlocks);
        blockchainProcessorState.setGetMoreBlocks(getMoreBlocks);
    }

    @Override
    public void suspendBlockchainDownloading() {
        setGetMoreBlocks(false);
        blockchainEvent.select(BlockchainEventType.literal(BlockchainEventType.SUSPEND_DOWNLOADING)).fire(blockchainConfig);
    }

    @Override
    public void resumeBlockchainDownloading() {
        setGetMoreBlocks(true);
        blockchainEvent.select(BlockchainEventType.literal(BlockchainEventType.RESUME_DOWNLOADING)).fire(blockchainConfig);
    }

    @Override
    public void waitUntilBlockchainDownloadingStops() {
        log.debug("Waiting until blockchain downloading stops.");
        globalSync.updateLock();
        globalSync.updateUnlock();
    }


    private String txToShortString(Transaction tx) {
        return Long.toUnsignedString(tx.getId()) + ", type " + tx.getType().getSpec() + ", sender " + Long.toUnsignedString(tx.getSenderId());
    }

    private String txsToString(Collection<? extends Transaction> c) {
        return "[" + c.stream().map(this::txToShortString).collect(Collectors.joining(",")) + "]";
    }

    private void revertUnconfirmedChanges(List<UnconfirmedTransaction> txsToRevert, RuntimeException selectTxEx) {
        Set<UnconfirmedTransaction> reverted = new HashSet<>();
        if (txsToRevert.isEmpty()) {
            return;
        }
        try {
            log.info("Revert back unconfirmed changes for the txs: {}", txsToString(txsToRevert));
            txsToRevert.forEach(e-> {
                transactionApplier.undoUnconfirmed(e);
                reverted.add(e);
            });
        } catch (RuntimeException e) {
            txsToRevert.removeAll(reverted);
            String notRevertedTransactionsString = txsToString(txsToRevert);
            int popOffHeight = Math.max(blockchain.getShardInitialBlock().getHeight(), blockchain.getHeight() - 1);
            log.error("Unable to undo unconfirmed changes for the transactions: " + notRevertedTransactionsString + ". Will do popOff to height " + popOffHeight);
            rollbackInconsistency(selectTxEx, popOffHeight);
            if (selectTxEx != null) { // do not lose original ex
                log.error("Previous error: Failed to select unconfirmed txs at height: " + blockchain.getHeight(), selectTxEx);
            }
            throw e;
        }
    }

    private void rollbackInconsistency(RuntimeException selectTxEx, int popOffHeight) {
        try {
            popOffTo(popOffHeight);
        } catch (RuntimeException popOffEx) {
            log.error("Pop off failed to height " + popOffHeight + ", inconsistent state changes may persist. Will perform node shutdown", selectTxEx); // do not lose original ex
            System.exit(1);
        }
    }

}