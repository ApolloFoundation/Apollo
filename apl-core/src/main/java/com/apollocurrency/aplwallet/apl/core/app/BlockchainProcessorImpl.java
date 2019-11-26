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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockchainEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ScanValidate;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.FilteringIterator;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.files.shards.ShardsDownloadService;
import com.apollocurrency.aplwallet.apl.core.files.statcheck.FileDownloadDecision;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPoll;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.shard.ShardImporter;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.apollocurrency.aplwallet.apl.util.task.TaskOrder;
import com.apollocurrency.aplwallet.apl.util.task.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class BlockchainProcessorImpl implements BlockchainProcessor {

    private static final String BACKGROUND_SERVICE_NAME = "BlockchainService";

    private final PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private final BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();

    private PeersService peers;
    private final DexService dexService;
    private BlockchainConfigUpdater blockchainConfigUpdater;

    private FullTextSearchService fullTextSearchProvider;

    private TaskDispatchManager taskDispatchManager;
    private Blockchain blockchain;
    private TransactionProcessor transactionProcessor;
    private TimeService timeService = CDI.current().select(TimeService.class).get();
    private final DatabaseManager databaseManager;

    private final ExecutorService networkService = Executors.newCachedThreadPool(new NamedThreadFactory("BlockchainProcessor:networkService"));


    private final int defaultNumberOfForkConfirmations = propertiesHolder.getIntProperty("apl.numberOfForkConfirmations");

    private int initialScanHeight;
    private volatile int lastRestoreTime = 0;
    private final Set<Long> prunableTransactions = new HashSet<>();
    private BlockValidator validator;
//    private final Listeners<Block, Event> blockListeners = new Listeners<>();
    private volatile Peer lastBlockchainFeeder;
    private final javax.enterprise.event.Event<Block> blockEvent;
    private final javax.enterprise.event.Event<List<Transaction>> txEvent;
    private final javax.enterprise.event.Event<BlockchainConfig> blockchainEvent;
    private final GlobalSync globalSync;
    private final DerivedTablesRegistry dbTables;
    private final ReferencedTransactionService referencedTransactionService;
    private final PhasingPollService phasingPollService;
    private final TransactionValidator transactionValidator;
    private final TransactionApplier transactionApplier;
    private final TrimService trimService;
    private final ShardImporter shardImporter;
    private final AplAppStatus aplAppStatus;
    private final BlockApplier blockApplier;
    private final ShardsDownloadService shardDownloader;
    private final PrunableMessageService prunableMessageService;
    private volatile int lastBlockchainFeederHeight;
    private volatile boolean getMoreBlocks = true;

    private volatile boolean isScanning;
    private volatile boolean isDownloading;
    private volatile boolean isProcessingBlock;
    private volatile boolean isRestoring;

    private TransactionProcessor lookupTransactionProcessor() {
        if (transactionProcessor == null) transactionProcessor = CDI.current().select(TransactionProcessorImpl.class).get();
        return transactionProcessor;
    }

    private Blockchain lookupBlockhain() {
        if (blockchain == null) blockchain = CDI.current().select(Blockchain.class).get();
        return blockchain;
    }

    private BlockchainConfigUpdater lookupBlockhainConfigUpdater() {
        if (blockchainConfigUpdater == null) blockchainConfigUpdater = CDI.current().select(BlockchainConfigUpdater.class).get();
        return blockchainConfigUpdater;
    }
    private TransactionalDataSource lookupDataSource() {
        return databaseManager.getDataSource();
    }

    private PeersService lookupPeersService() {
        if (peers == null) peers = CDI.current().select(PeersService.class).get();
        return peers;
    }

    //private final Runnable getMoreBlocksThread = new GetMoreBlocksThread();


    /**
     * Task to restore prunable data for downloaded blocks
     */
    private class RestorePrunableDataTask implements Runnable {

        @Override
        public void run() {
            Peer peer = null;
            try {
                //
                // Locate an archive peer
                //
                List<Peer> peersList = lookupPeersService().getPeers(chkPeer -> chkPeer.providesService(Peer.Service.PRUNABLE) &&
                        !chkPeer.isBlacklisted() && chkPeer.getAnnouncedAddress() != null);
                while (!peersList.isEmpty()) {
                    Peer chkPeer = peersList.get(ThreadLocalRandom.current().nextInt(peersList.size()));
                    if (chkPeer.getState() != PeerState.CONNECTED) {
                        lookupPeersService().connectPeer(chkPeer);
                    }
                    if (chkPeer.getState() == PeerState.CONNECTED) {
                        peer = chkPeer;
                        break;
                    }
                }
                if (peer == null) {
                    log.debug("Cannot find any archive peers");
                    return;
                }
                log.debug("Connected to archive peer " + peer.getHost());
                //
                // Make a copy of the prunable transaction list so we can remove entries
                // as we process them while still retaining the entry if we need to
                // retry later using a different archive peer
                //
                Set<Long> processing;
                synchronized (prunableTransactions) {
                    processing = new HashSet<>(prunableTransactions.size());
                    processing.addAll(prunableTransactions);
                }
                log.debug("Need to restore " + processing.size() + " pruned data");
                //
                // Request transactions in batches of 100 until all transactions have been processed
                //
                while (!processing.isEmpty()) {
                    //
                    // Get the pruned transactions from the archive peer
                    //
                    JSONObject request = new JSONObject();
                    JSONArray requestList = new JSONArray();
                    synchronized (prunableTransactions) {
                        Iterator<Long> it = processing.iterator();
                        while (it.hasNext()) {
                            long id = it.next();
                            requestList.add(Long.toUnsignedString(id));
                            it.remove();
                            if (requestList.size() == 100)
                                break;
                        }
                    }
                    request.put("requestType", "getTransactions");
                    request.put("transactionIds", requestList);
                    request.put("chainId", blockchainConfig.getChain().getChainId());
                    JSONObject response;
                    try {
                        response = peer.send(JSON.prepareRequest(request), blockchainConfig.getChain().getChainId());
                    } catch (PeerNotConnectedException ex) {
                        response = null;
                    }
                    if (response == null) {
                        return;
                    }
                    //
                    // Restore the prunable data
                    //
                    JSONArray transactions = (JSONArray)response.get("transactions");
                    if (transactions == null || transactions.isEmpty()) {
                        return;
                    }
                    List<Transaction> processed = lookupTransactionProcessor().restorePrunableData(transactions);
                    //
                    // Remove transactions that have been successfully processed
                    //
                    synchronized (prunableTransactions) {
                        processed.forEach(transaction -> prunableTransactions.remove(transaction.getId()));
                    }
                }
                log.debug("Done retrieving prunable transactions from " + peer.getHost());
            } catch (AplException.ValidationException e) {
                log.error("Peer " + peer.getHost() + " returned invalid prunable transaction", e);
                peer.blacklist(e);
            } catch (RuntimeException e) {
                log.error("Unable to restore prunable data", e);
            } finally {
                isRestoring = false;
                log.debug("Remaining " + prunableTransactions.size() + " pruned transactions");
            }
        }
    }

    @Inject
    public BlockchainProcessorImpl(BlockValidator validator, Event<Block> blockEvent,
                                   GlobalSync globalSync, DerivedTablesRegistry dbTables,
                                   ReferencedTransactionService referencedTransactionService, PhasingPollService phasingPollService,
                                   TransactionValidator transactionValidator,
                                   TransactionApplier transactionApplier,
                                   TrimService trimService, DatabaseManager databaseManager, DexService dexService,
                                   BlockApplier blockApplier, AplAppStatus aplAppStatus,
                                   ShardsDownloadService shardDownloader,
                                   ShardImporter importer, PrunableMessageService prunableMessageService,
                                   TaskDispatchManager taskDispatchManager, Event<List<Transaction>> txEvent, Event<BlockchainConfig> blockchainEvent) {
        this.validator = validator;
        this.blockEvent = blockEvent;
        this.globalSync = globalSync;
        this.dbTables = dbTables;
        this.trimService = trimService;
        this.phasingPollService = phasingPollService;
        this.transactionValidator = transactionValidator;
        this.transactionApplier = transactionApplier;
        this.referencedTransactionService = referencedTransactionService;
        this.databaseManager = databaseManager;
        this.dexService = dexService;
        this.blockApplier = blockApplier;
        this.aplAppStatus = aplAppStatus;
        this.shardDownloader = shardDownloader;
        this.shardImporter = importer;
        this.prunableMessageService = prunableMessageService;
        this.taskDispatchManager = taskDispatchManager;
        this.txEvent = txEvent;
        this.blockchainEvent = blockchainEvent;

        configureBackgroundTasks();

    }

    private void configureBackgroundTasks() {

        TaskDispatcher dispatcher = taskDispatchManager.newBackgroundDispatcher(BACKGROUND_SERVICE_NAME);

        Task blockChainInitTask = Task.builder()
                .name("BlockchainInit")
                .task(() -> {
                    continuedDownloadOrTryImportGenesisShard(); // continue blockchain automatically or try import genesis / shard data
                    if (blockchain.getShardInitialBlock() != null) { // prevent NPE on empty node
                        trimService.init(blockchain.getHeight(), blockchain.getShardInitialBlock().getHeight()); // try to perform all not performed trims
                    } else {
                        trimService.resetTrim();
                    }
                    if (propertiesHolder.getBooleanProperty("apl.forceScan")) {
                        scan(0, propertiesHolder.getBooleanProperty("apl.forceValidate"));
                    } else {
                        boolean rescan;
                        boolean validate;
                        int height;
                        try (Connection con = lookupDataSource().getConnection();
                             Statement stmt = con.createStatement();
                             ResultSet rs = stmt.executeQuery("SELECT * FROM scan")) {
                            rs.next();
                            rescan = rs.getBoolean("rescan");
                            validate = rs.getBoolean("validate");
                            height = rs.getInt("height");
                        } catch (SQLException e) {
                            throw new RuntimeException(e.toString(), e);
                        }
                        if (rescan) {
                            scan(height, validate);
                        }
                    }
                }).build();

        dispatcher.invokeInit(blockChainInitTask);


        if (!propertiesHolder.isLightClient() && !propertiesHolder.isOffline()) {
            Task moreBlocksTask = Task.builder()
                    .name("GetMoreBlocks")
                    .delay(250)
                    .initialDelay(250)
                    .task(new GetMoreBlocksThread())
                    .build();

            dispatcher.schedule(moreBlocksTask);
        }
    }

    private FullTextSearchService lookupFullTextSearchProvider() {
        if (fullTextSearchProvider == null) {
            fullTextSearchProvider = CDI.current().select(FullTextSearchService.class).get();
        }
        return fullTextSearchProvider;
    }
    @Override
    public Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    @Override
    public int getLastBlockchainFeederHeight() {
        return lastBlockchainFeederHeight;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public int getInitialScanHeight() {
        return initialScanHeight;
    }

    @Override
    public boolean isDownloading() {
        return isDownloading;
    }

    @Override
    public boolean isProcessingBlock() {
        return isProcessingBlock;
    }

    @Override
    public int getMinRollbackHeight() {
        int minRollBackHeight = trimService.getLastTrimHeight() > 0 ? trimService.getLastTrimHeight()
                : Math.max(lookupBlockhain().getHeight() - propertiesHolder.MAX_ROLLBACK(), 0);
        log.trace("minRollbackHeight  = {}", minRollBackHeight);
        return minRollBackHeight;
    }

    @Override
    public void processPeerBlock(JSONObject request) throws AplException {
        globalSync.updateLock();
        try {
            Block lastBlock = lookupBlockhain().getLastBlock();
            long peerBlockPreviousBlockId = Convert.parseUnsignedLong((String) request.get("previousBlock"));
            log.trace("Timeout: peerBlock{},ourBlock{}", request.get("timeout"), lastBlock.getTimeout());
            log.trace("Timestamp: peerBlock{},ourBlock{}", request.get("timestamp"), lastBlock.getTimestamp());
            log.trace("PrevId: peerBlock{},ourBlock{}", peerBlockPreviousBlockId, lastBlock.getPreviousBlockId());
            // peer block is the next block in our blockchain
            if (peerBlockPreviousBlockId == lastBlock.getId()) {
                log.debug("push peer last block");
                Block block = BlockImpl.parseBlock(request);
                pushBlock(block);
            } else if (peerBlockPreviousBlockId == lastBlock.getPreviousBlockId()) { //peer block is a candidate to replace our last block
                Block block = BlockImpl.parseBlock(request);
                //try to replace our last block by peer block only when timestamp of peer block is less than timestamp of our block or when
                // timestamps are equal but timeout of peer block is greater, so that peer block is better.
                if (((block.getTimestamp() < lastBlock.getTimestamp()
                        || block.getTimestamp() == lastBlock.getTimestamp() && block.getTimeout() > lastBlock.getTimeout()))) {
                    log.debug("Need to replace block");
                    Block lb = lookupBlockhain().getLastBlock();
                    if (lastBlock.getId() != lb.getId()) {
                        log.debug("Block changed: expected: id {} height: {} generator: {}, got id {}, height {}, generator {} ", lastBlock.getId(),
                                lastBlock.getHeight(), Convert2.rsAccount(lastBlock.getGeneratorId()), lb.getId(), lb.getHeight(),
                                Convert2.rsAccount(lb.getGeneratorId()));
                        return; // blockchain changed, ignore the block
                    }
                    Block previousBlock = lookupBlockhain().getBlock(lastBlock.getPreviousBlockId());
                    lastBlock = popOffTo(previousBlock).get(0);
                    try {
                        pushBlock(block);
                        log.debug("Pushed better peer block: id {} height: {} generator: {}",
                                block.getId(),
                                block.getHeight(),
                                Convert2.rsAccount(block.getGeneratorId()));
                        lookupTransactionProcessor().processLater(lastBlock.getOrLoadTransactions());
                        log.debug("Last block " + lastBlock.getStringId() + " was replaced by " + block.getStringId());
                    }
                    catch (BlockNotAcceptedException e) {
                        log.debug("Replacement block failed to be accepted, pushing back our last block");
                        pushBlock(lastBlock);
                        lookupTransactionProcessor().processLater(block.getOrLoadTransactions());
                    }
                }
            }// else ignore the block
        }
        finally {
            globalSync.updateUnlock();
        }
    }

    @Override
    public List<Block> popOffTo(int height) {
        if (height <= 0) {
            fullReset();
        } else if (height < lookupBlockhain().getHeight()) {
            return popOffTo(lookupBlockhain().getBlockAtHeight(height));
        }
        return Collections.emptyList();
    }

    @Override
    public void fullReset() {
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
                    trimService.trimDerivedTables(0, false);
                    DirProvider dirProvider = RuntimeEnvironment.getInstance().getDirProvider();
                    Path dataExportDir = dirProvider.getDataExportDir();
                    FileUtils.clearDirectorySilently(dataExportDir);
                    FileUtils.deleteFilesByPattern(dirProvider.getDbDir(), new String[]{".zip", ".h2.db"}, new String[]{"-shard-"});
                    dataSource.commit(false);
                    lookupBlockhainConfigUpdater().rollback(0);
                }
                catch (Exception e) {
                    log.error(e.toString(), e);
                    dataSource.rollback(false);
                }
                finally {
                    dataSource.commit();
                }
                continuedDownloadOrTryImportGenesisShard();// continue blockchain automatically or try import genesis / shard data
            } finally {
                resumeBlockchainDownloading();
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public int restorePrunedData() {
        TransactionalDataSource dataSource = lookupDataSource();
        try (Connection con = dataSource.begin()) {
            int now = timeService.getEpochTime();
            int minTimestamp = Math.max(1, now - blockchainConfig.getMaxPrunableLifetime());
            int maxTimestamp = Math.max(minTimestamp, now - blockchainConfig.getMinPrunableLifetime()) - 1;
            List<PrunableTransaction> transactionList =
                    lookupBlockhain().findPrunableTransactions(con, minTimestamp, maxTimestamp);
            transactionList.forEach(prunableTransaction -> {
                long id = prunableTransaction.getId();
                if ((prunableTransaction.hasPrunableAttachment() && prunableTransaction.getTransactionType().isPruned(id)) ||
                        prunableMessageService.isPruned(id, prunableTransaction.hasPrunablePlainMessage(), prunableTransaction.hasPrunableEncryptedMessage())) {
                    synchronized (prunableTransactions) {
                        prunableTransactions.add(id);
                    }
                }
            });
            if (!prunableTransactions.isEmpty()) {
                lastRestoreTime = 0;
            }
            dataSource.commit();
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e.toString(), e);
        }
        synchronized (prunableTransactions) {
            return prunableTransactions.size();
        }
    }

    @Override
    public Transaction restorePrunedTransaction(long transactionId) {
        Transaction transaction = lookupBlockhain().getTransaction(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        boolean isPruned = false;
        for (AbstractAppendix appendage : transaction.getAppendages(true)) {
            if ((appendage instanceof Prunable) &&
                    !((Prunable)appendage).hasPrunableData()) {
                isPruned = true;
                break;
            }
        }
        if (!isPruned) {
            return transaction;
        }
        List<Peer> peersList = lookupPeersService().getPeers(chkPeer -> chkPeer.providesService(Peer.Service.PRUNABLE) &&
                !chkPeer.isBlacklisted() && chkPeer.getAnnouncedAddress() != null);
        if (peersList.isEmpty()) {
            log.debug("Cannot find any archive peers");
            return null;
        }
        JSONObject json = new JSONObject();
        JSONArray requestList = new JSONArray();
        requestList.add(Long.toUnsignedString(transactionId));
        json.put("requestType", "getTransactions");
        json.put("transactionIds", requestList);
        json.put("chainId", blockchainConfig.getChain().getChainId());
        JSONStreamAware request = JSON.prepareRequest(json);
        for (Peer peer : peersList) {
            if (peer.getState() != PeerState.CONNECTED) {
                lookupPeersService().connectPeer(peer);
            }
            if (peer.getState() != PeerState.CONNECTED) {
                continue;
            }
            log.debug("Connected to archive peer " + peer.getHost());
            JSONObject response;
            try {
                response = peer.send(request, blockchainConfig.getChain().getChainId());
            } catch (PeerNotConnectedException ex) {
                response = null;
            }
            if (response == null) {
                continue;
            }
            JSONArray transactions = (JSONArray)response.get("transactions");
            if (transactions == null || transactions.isEmpty()) {
                continue;
            }
            try {
                List<Transaction> processed = lookupTransactionProcessor().restorePrunableData(transactions);
                if (processed.isEmpty()) {
                    continue;
                }
                synchronized (prunableTransactions) {
                    prunableTransactions.remove(transactionId);
                }
                return processed.get(0);
            } catch (AplException.NotValidException e) {
                log.error("Peer " + peer.getHost() + " returned invalid prunable transaction", e);
                peer.blacklist(e);
            }
        }
        return null;
    }

    @Override
    public List<Transaction> getExpectedTransactions(Filter<Transaction> filter) {
        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        List<Transaction> result = new ArrayList<>();
        globalSync.readLock();
        try {
            List<Transaction> phasedTransactions = phasingPollService.getFinishingTransactions(blockchain.getHeight() + 1);
            for (Transaction phasedTransaction : phasedTransactions) {
                try {
                    transactionValidator.validate(phasedTransaction);
                    if (!phasedTransaction.attachmentIsDuplicate(duplicates, false) && filter.test(phasedTransaction)) {
                        result.add(phasedTransaction);
                    }
                } catch (AplException.ValidationException ignore) {}
            }

            selectUnconfirmedTransactions(duplicates, blockchain.getLastBlock(), -1, Integer.MAX_VALUE).forEach(
                    unconfirmedTransaction -> {
                        Transaction transaction = unconfirmedTransaction.getTransaction();
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
        TransactionalDataSource dataSource = lookupDataSource();
        try (Connection con = dataSource.getConnection()) {
            lookupBlockhain().saveBlock(con, block);
            lookupBlockhain().setLastBlock(block);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void continuedDownloadOrTryImportGenesisShard() {
        Block lastBlock = lookupBlockhain().getLastBlock(); // blockchain should be initialized independently
        if (lastBlock != null) {
            // continue blockchain automatically
            log.info("Genesis block already in database");
            blockchain.deleteBlocksFromHeight(lastBlock.getHeight() + 1);
            popOffTo(lastBlock);
            log.info("Last block height: " + lastBlock.getHeight());
            resumeBlockchainDownloading(); // turn ON blockchain downloading
            scheduleOneScan();
            return;
        }
        // NEW START-UP logic, try import genesis OR start downloading shard zip data
        suspendBlockchainDownloading(); // turn off automatic blockchain downloading
        long timeDelay = 4000L;
        try {
            log.warn("----!!!>>> NODE IS WAITING FOR '{}' milliseconds about 'shard/no_shard decision' " +
                    "and proceeding with necessary data later by receiving NO_SHARD / SHARD_PRESENT event....", timeDelay);
            // try make delay before PeersService are up and running
            Thread.sleep(timeDelay); // milli-seconds to wait for PeersService initialization
            // ignore result, because async event is expected/received by 'ShardDownloadPresenceObserver' component
            FileDownloadDecision downloadDecision = shardDownloader.tryDownloadLastGoodShard();
            disableScheduleOneScan();
            log.debug("NO_SHARD/SHARD_PRESENT decision was = '{}'", downloadDecision);
        } catch (InterruptedException e) {
            log.error("main BlockchainProcessorImpl thread was interrupted, EXITING...");
            System.exit(-1);
        }

/*
        // PREVIOUS start-up LOGIC
        log.info("Genesis block not in database, starting from scratch");
        TransactionalDataSource dataSource = lookupDataSource();
        Connection con = dataSource.begin();
        try {
            // we should start here shard downloading
            // Maybe better to rename this method
            Block genesisBlock = Genesis.newGenesisBlock();
            addBlock(genesisBlock);
            initialBlock = genesisBlock.getId();
            Genesis.apply(false);
            for (DerivedTableInterface table : dbTables.getDerivedTables()) {
                table.createSearchIndex(con);
            }
            blockchain.commit(genesisBlock);
            dataSource.commit();
            log.debug("Saved Genesis block = {}", genesisBlock);
        } catch (SQLException e) {
            dataSource.rollback();
            log.info(e.getMessage());
            throw new RuntimeException(e.toString(), e);
        }
*/
    }

    public void scheduleOneScan() {
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        String scanProperty = optionDAO.get("require-scan");
        if (scanProperty == null) {
            optionDAO.set("require-scan", "false");
            scheduleScan(0, false);
        }
    }

    private void disableScheduleOneScan() {
        log.debug("disableScheduleOneScan...");
        OptionDAO optionDAO = new OptionDAO(databaseManager);
        optionDAO.set("require-scan", "false");
    }

    private void pushBlock(final Block block) throws BlockNotAcceptedException {
        int curTime = timeService.getEpochTime();
        log.trace("push new block, prev_id = '{}', cutTime={}", block.getPreviousBlockId(), curTime);
        long startTime = System.currentTimeMillis();
        globalSync.writeLock();
        long lockAquireTime = System.currentTimeMillis() - startTime;
        try {
            Block previousLastBlock = null;
            TransactionalDataSource dataSource = lookupDataSource();
            dataSource.begin();
            try {
                previousLastBlock = lookupBlockhain().getLastBlock();

                validator.validate(block, previousLastBlock, curTime);

                long nextHitTime = Generator.getNextHitTime(previousLastBlock.getId(), curTime);
                if (nextHitTime > 0 && block.getTimestamp() > nextHitTime + 1) {
                    String msg = "Rejecting block " + block.getStringId() + " at height " + previousLastBlock.getHeight()
                            + " block timestamp " + block.getTimestamp() + " next hit time " + nextHitTime
                            + " current time " + curTime;
                    log.debug(msg);
                    Generator.setDelay(-propertiesHolder.FORGING_SPEEDUP());
                    throw new BlockOutOfOrderException(msg, block);
                }

                Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
                List<Transaction> validPhasedTransactions = new ArrayList<>();
                List<Transaction> invalidPhasedTransactions = new ArrayList<>();
                validatePhasedTransactions(block, previousLastBlock, validPhasedTransactions, invalidPhasedTransactions, duplicates);
                validateTransactions(block, previousLastBlock, curTime, duplicates, previousLastBlock.getHeight() >= Constants.LAST_CHECKSUM_BLOCK);

                block.setPrevious(previousLastBlock);
                log.trace("fire block on = {}, id = '{}', '{}'", block.getHeight(), block.getId(), BlockEventType.BEFORE_BLOCK_ACCEPT.name());
                blockEvent.select(literal(BlockEventType.BEFORE_BLOCK_ACCEPT)).fire(block);
                lookupTransactionProcessor().requeueAllUnconfirmedTransactions();
                addBlock(block);

                accept(block, validPhasedTransactions, invalidPhasedTransactions, duplicates);

                blockchain.commit(block);
                dataSource.commit(false);
                log.trace("committed block on = {}, id = '{}'", block.getHeight(), block.getId());
            } catch (Exception e) {
                dataSource.rollback(false); // do not close current transaction
                log.error("PushBlock, error:", e);
                popOffTo(previousLastBlock); // do in current transaction
                blockchain.setLastBlock(previousLastBlock);
                throw e;
            } finally {
                dataSource.commit(); // finally close transaction
            }
            log.trace("fire block on = {}, id = '{}', '{}'", block.getHeight(), block.getId(), BlockEventType.AFTER_BLOCK_ACCEPT.name());
            blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(block);
        } finally {
            globalSync.writeUnlock();
        }
        if (block.getTimestamp() >= curTime - 600) {
            log.debug("From pushBlock, Send block to peers: height: {} id: {} generator:{}", block.getHeight(), Long.toUnsignedString(block.getId()),
                    Convert2.rsAccount(block.getGeneratorId()));
            lookupPeersService().sendToSomePeers(block);
        }
        log.trace("fire block on = {}, id = '{}', '{}'", block.getHeight(), Long.toUnsignedString(block.getId()), BlockEventType.BLOCK_PUSHED.name());
        blockEvent.select(literal(BlockEventType.BLOCK_PUSHED)).fire(block); // send sync event to TrimObserver component
        blockEvent.select(literal(BlockEventType.BLOCK_PUSHED)).fireAsync(block); // send async event to other components
        log.debug("Push block at height {} tx cnt: {} took {} ms (lock acquiring: {} ms)",
                block.getHeight(), block.getTransactions().size(), System.currentTimeMillis() - startTime, lockAquireTime);
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
                                            Map<TransactionType, Map<String, Integer>> duplicates) {
        int height = prevBlock.getHeight();

        List<Transaction> transactions = new ArrayList<>(phasingPollService.getFinishingTransactions(prevBlock.getHeight() + 1));

        transactions.addAll(phasingPollService.getFinishingTransactionsByTime(prevBlock.getTimestamp(), currentBlock.getTimestamp()));

        for (Transaction phasedTransaction : transactions) {
            //TODO check it in the sql.
            if (phasingPollService.getResult(phasedTransaction.getId()) != null) {
                continue;
            }
            try {
                transactionValidator.validate(phasedTransaction);
                if (!phasedTransaction.attachmentIsDuplicate(duplicates, false)) {
                    validPhasedTransactions.add(phasedTransaction);
                } else {
                    log.debug("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " is duplicate, will not apply");
                    invalidPhasedTransactions.add(phasedTransaction);
                }
            } catch (AplException.ValidationException e) {
                log.debug("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " no longer passes validation: "
                        + e.getMessage() + ", will not apply");
                invalidPhasedTransactions.add(phasedTransaction);
            }
        }
    }

    private void validateTransactions(Block block, Block previousLastBlock, int curTime, Map<TransactionType, Map<String, Integer>> duplicates,
                                      boolean fullValidation) throws BlockNotAcceptedException {
        long payloadLength = 0;
        long calculatedTotalAmount = 0;
        long calculatedTotalFee = 0;
        MessageDigest digest = Crypto.sha256();
        boolean hasPrunedTransactions = false;
        for (Transaction transaction : block.getOrLoadTransactions()) {
            if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
                throw new BlockOutOfOrderException("Invalid transaction timestamp: " + transaction.getTimestamp()
                        + ", current time is " + curTime, block);
            }
            if (!transaction.verifySignature()) {
                throw new TransactionNotAcceptedException("Transaction signature verification failed at height " + previousLastBlock.getHeight(), transaction);
            }
            if (fullValidation) {
                if (transaction.getTimestamp() > block.getTimestamp() + Constants.MAX_TIMEDRIFT
                        || transaction.getExpiration() < block.getTimestamp()) {
                    throw new TransactionNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                            + ", current time is " + curTime + ", block timestamp is " + block.getTimestamp(), transaction);
                }
                if (lookupBlockhain().hasTransaction(transaction.getId(), previousLastBlock.getHeight())) {
                    throw new TransactionNotAcceptedException("Transaction is already in the blockchain", transaction);
                }
                if (transaction.referencedTransactionFullHash() != null && !referencedTransactionService.hasAllReferencedTransactions(transaction, previousLastBlock.getHeight() + 1)) {
                    throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
                            + transaction.getReferencedTransactionFullHash(), transaction);
                }
                if (transaction.getVersion() != getTransactionVersion(previousLastBlock.getHeight())) {
                    throw new TransactionNotAcceptedException("Invalid transaction version " + transaction.getVersion()
                            + " at height " + previousLastBlock.getHeight(), transaction);
                }
                if (transaction.getId() == 0L) {
                    throw new TransactionNotAcceptedException("Invalid transaction id 0", transaction);
                }
                try {
                    transactionValidator.validate(transaction);
                } catch (AplException.ValidationException e) {
                    throw new TransactionNotAcceptedException(e.getMessage(), transaction);
                }
            }
            if (transaction.attachmentIsDuplicate(duplicates, true)) {
                throw new TransactionNotAcceptedException("Transaction is a duplicate", transaction);
            }
            if (!hasPrunedTransactions) {
                for (Appendix appendage : transaction.getAppendages()) {
                    if ((appendage instanceof Prunable) && !((Prunable)appendage).hasPrunableData()) {
                        hasPrunedTransactions = true;
                        break;
                    }
                }
            }
            calculatedTotalAmount += transaction.getAmountATM();
            calculatedTotalFee += transaction.getFeeATM();
            payloadLength += transaction.getFullSize();
            digest.update(((TransactionImpl)transaction).bytes());
        }
        if (calculatedTotalAmount != block.getTotalAmountATM() || calculatedTotalFee != block.getTotalFeeATM()) {
            throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals", block);
        }
        if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
            throw new BlockNotAcceptedException("Payload hash doesn't match", block);
        }
        if (hasPrunedTransactions ? payloadLength > block.getPayloadLength() : payloadLength != block.getPayloadLength()) {
            throw new BlockNotAcceptedException("Transaction payload length " + payloadLength + " does not match block payload length "
                    + block.getPayloadLength(), block);
        }
    }

    private void accept(Block block, List<Transaction> validPhasedTransactions, List<Transaction> invalidPhasedTransactions,
                        Map<TransactionType, Map<String, Integer>> duplicates) throws TransactionNotAcceptedException {
        long start = System.currentTimeMillis();
        try {
            log.debug(":accept: Accepting block: {} height: {}", block.getId(), block.getHeight());
            isProcessingBlock = true;
            for (Transaction transaction : block.getOrLoadTransactions()) {
                if (! transactionApplier.applyUnconfirmed(transaction)) {
                    throw new TransactionNotAcceptedException("Double spending", transaction);
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
            for (Transaction transaction : block.getOrLoadTransactions()) {
                try {
                    transactionApplier.apply(transaction);
                    if (transaction.getTimestamp() > fromTimestamp) {
                        for (AbstractAppendix appendage : transaction.getAppendages(true)) {
                            if ((appendage instanceof Prunable) &&
                                        !((Prunable)appendage).hasPrunableData()) {
                                synchronized (prunableTransactions) {
                                    prunableTransactions.add(transaction.getId());
                                }
                                lastRestoreTime = 0;
                                break;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    log.error(e.toString(), e);
                    throw new BlockchainProcessor.TransactionNotAcceptedException(e, transaction);
                }
            }
            SortedSet<Transaction> possiblyApprovedTransactions = new TreeSet<>(finishingTransactionsComparator);
            log.trace(":accept: validate all block transactions");
            block.getOrLoadTransactions().forEach(transaction -> {
                phasingPollService.getLinkedPhasedTransactions(transaction.getFullHash()).forEach(phasedTransaction -> {
                    if ((phasedTransaction.getPhasing().getFinishHeight() > block.getHeight()
                            || phasedTransaction.getPhasing().getClass() == PhasingAppendixV2.class
                            && ((PhasingAppendixV2) phasedTransaction.getPhasing()).getFinishTime() > block.getTimestamp()
                    )
                            && phasingPollService.getResult(phasedTransaction.getId()) == null) {
                        possiblyApprovedTransactions.add(phasedTransaction);
                    }
                });
                if (transaction.getType() == Messaging.PHASING_VOTE_CASTING && !transaction.attachmentIsPhased()) {
                    MessagingPhasingVoteCasting voteCasting = (MessagingPhasingVoteCasting)transaction.getAttachment();
                    voteCasting.getTransactionFullHashes().forEach(hash -> {
                        PhasingPoll phasingPoll = phasingPollService.getPoll(Convert.fullHashToId(hash));
                        if (phasingPoll.allowEarlyFinish()
                                && (phasingPoll.getFinishHeight() > block.getHeight()
                                || phasingPoll.getFinishTime() > block.getTimestamp())
                                && phasingPollService.getResult(phasingPoll.getId()) == null) {
                            possiblyApprovedTransactions.add(lookupBlockhain().getTransaction(phasingPoll.getId()));
                        }
                    });
                }
            });
            log.trace(":accept: validate Valid phasing transactions");
            validPhasedTransactions.forEach(phasedTransaction -> {
                if (phasedTransaction.getType() == Messaging.PHASING_VOTE_CASTING) {
                    PhasingPollResult result = phasingPollService.getResult(phasedTransaction.getId());
                    if (result != null && result.isApproved()) {
                        MessagingPhasingVoteCasting phasingVoteCasting = (MessagingPhasingVoteCasting) phasedTransaction.getAttachment();
                        phasingVoteCasting.getTransactionFullHashes().forEach(hash -> {
                            PhasingPoll phasingPoll = phasingPollService.getPoll(Convert.fullHashToId(hash));
                            if (phasingPoll.allowEarlyFinish()
                                    && (phasingPoll.getFinishHeight() > block.getHeight()
                                    || phasingPoll.getFinishTime() > block.getTimestamp())
                                    && phasingPollService.getResult(phasingPoll.getId()) == null) {
                                possiblyApprovedTransactions.add(lookupBlockhain().getTransaction(phasingPoll.getId()));
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
                        transactionValidator.validate(transaction);
                        phasingPollService.tryCountVotes(transaction, duplicates);
                    } catch (AplException.ValidationException e) {
                        log.debug("At height " + block.getHeight() + " phased transaction " + transaction.getStringId()
                                + " no longer passes validation: " + e.getMessage() + ", cannot finish early");
                    }
//                }
            });
            log.trace(":accept: dex service block.");
            try {
                dexService.closeOverdueOrders(block.getTimestamp());
                dexService.closeOverdueContracts(block.getTimestamp());
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
            if (block.getOrLoadTransactions().size() > 0) {
                txEvent.select(TxEventType.literal(TxEventType.ADDED_CONFIRMED_TRANSACTIONS)).fire(block.getOrLoadTransactions());
            }
            log.trace(":accept: Account ledger commit.");
            AccountLedger.commitEntries();
            log.trace(":accept: that's it.");
        } finally {
            isProcessingBlock = false;
            AccountLedger.clearEntries();
            log.trace("Accepting block DONE: {} height: {} processing time ms: {}", block.getId(), block.getHeight(), System.currentTimeMillis() - start);
        }
    }

    private static final Comparator<Transaction> finishingTransactionsComparator = Comparator
            .comparingInt(Transaction::getHeight)
            .thenComparingInt(Transaction::getIndex)
            .thenComparingLong(Transaction::getId);

    public List<Block> popOffTo(Block commonBlock) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        globalSync.writeLock();
        try {
            if (!dataSource.isInTransaction()) {
                try {
                    dataSource.begin();
                    return popOffToInTransaction(commonBlock,dataSource);
                } finally {
                    dataSource.commit();
                }
            } else {
                return popOffToInTransaction(commonBlock,dataSource);
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    public List<Block> popOffToInTransaction(Block commonBlock, TransactionalDataSource dataSource) {
        int minRollbackHeight = getMinRollbackHeight();
        int commonBlockHeight = commonBlock.getHeight();
        log.debug(">> popOffToInTransaction() to height = {}, minRollbackHeight={}", commonBlockHeight, minRollbackHeight);
        if (commonBlockHeight < minRollbackHeight) {
            log.info("Rollback to height " + commonBlockHeight + " not supported, will do a full rescan");

            // usually = 0 on full node (or on sharded node without any shard yet)
            // > 0 on sharded node with one or more shards
            int shardInitialHeight = blockchain.getShardInitialBlock().getHeight();
            if (commonBlockHeight < shardInitialHeight) {
                // when we have a shard on node, we can't scan below 'latest' snapshot block in main db
                log.warn("Popping the blocks off that before the last shard block is not supported (height={} < shardInitialHeight={})",
                        commonBlockHeight, shardInitialHeight);
            } else {
                // check shard conditions...
                HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
                boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
                boolean shardingEnabled = currentConfig.isShardingEnabled();
                log.debug("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
                if (shardInitialHeight != 0 && shardingEnabled && !isShardingOff) {
                    // sharding is enabled and turned ON
                    log.warn("DO NOT do 'popOffWithRescan' to height(+1) = {} / shardInitialHeight={}, it NEEDs refactoring...",
                            commonBlockHeight + 1,  shardInitialHeight);
//                    popOffWithRescan(commonBlockHeight + 1); // YL: needs more investigation and scan refactoring
                } else {
                    // sharding is DISABLED and turned OFF, FULL DB mode
                    log.warn("DO 'popOffWithRescan' to height(+1) = {}...", commonBlockHeight + 1);
                    popOffWithRescan(commonBlockHeight + 1); // 'full node' can go to full rescan here
                }
            }
            return Collections.emptyList();
        }
        if (!lookupBlockhain().hasBlock(commonBlock.getId())) {
            log.debug("Block " + commonBlock.getStringId() + " not found in blockchain, nothing to pop off");
            return Collections.emptyList();
        }
        List<Block> poppedOffBlocks = new ArrayList<>();
        try {
            Block block = lookupBlockhain().getLastBlock();
            ((BlockImpl) block).loadTransactions();
            log.debug("ROLLBACK from block " + block.getStringId() + " at height " + block.getHeight()
                    + " to " + commonBlock.getStringId() + " at " + commonBlockHeight);
            while (block.getId() != commonBlock.getId() && block.getHeight() > 0) {
                poppedOffBlocks.add(block);
                block = popLastBlock();
            }
            long rollbackStartTime = System.currentTimeMillis();
            log.debug("Start rollback for tables=[{}]", dbTables.getDerivedTables().size());
            for (DerivedTableInterface table : dbTables.getDerivedTables()) {
                long start = System.currentTimeMillis();
                table.rollback(commonBlockHeight);
                log.debug("rollback for table={} in {} ms", table.getName(), System.currentTimeMillis() - start);
            }
            log.debug("Total rollback time: {} ms", System.currentTimeMillis() - rollbackStartTime);
            dataSource.clearCache();
            dataSource.commit(false); // should happen definitely otherwise
            log.debug("<< popOffToInTransaction() blocks=[{}] at height={}", poppedOffBlocks.size(), commonBlockHeight);
        }
        catch (RuntimeException e) {
            log.error("Error popping off to {}, cause {}", commonBlockHeight, e.toString());
            dataSource.rollback(false);
            if (blockchain != null) { //prevent NPE on shutdown
                Block lastBlock = blockchain.findLastBlock();
                if (lastBlock == null){
                    log.error("Error popping off, lastBlock is NULL.", e);
                } else {
                    blockchain.setLastBlock(lastBlock);
                    popOffToInTransaction(lastBlock, dataSource);
                }
            }
            throw e;
        }
        return poppedOffBlocks;
    }

    private Block popLastBlock() {
        Block block = lookupBlockhain().getLastBlock();
        if (block.getHeight() == 0) {
            throw new RuntimeException("Cannot pop off genesis block");
        }
        Block previousBlock = blockchain.deleteBlocksFrom(block.getId());
        ((BlockImpl)previousBlock).loadTransactions();
        lookupBlockhain().setLastBlock(previousBlock);
        blockEvent.select(literal(BlockEventType.BLOCK_POPPED)).fire(block);
        return previousBlock;
    }

    private void popOffWithRescan(int height) {
        log.debug(">> popOffWithRescan to height = " + height);
        globalSync.writeLock();
        try {
            int scanHeight = 0;
            int shardInitialHeight = blockchain.getShardInitialBlock().getHeight();
            if ( shardInitialHeight > 0 ) {
                scanHeight = Math.max(height, shardInitialHeight);
            }
            log.debug("Set scanHeight={}, shard's initialBlockHeight={}, currentHeight={}", scanHeight, shardInitialHeight, height);
            try {
                scheduleScan(scanHeight, false);
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
                lookupBlockhain().setLastBlock(lastBLock);
                lookupBlockhainConfigUpdater().rollback(lastBLock.getHeight());
                log.debug("Blockchain config updated, lastBlockId={} at height={}", lastBLock.getId(), lastBLock.getHeight());
            } catch (Exception e) {
                // just for logging possible hidden error
                log.error("popOffWithRescan Error", e);
            } finally {
                try {
                    scan(scanHeight, false);
                }catch (BlockchainScanException e){
                    log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + e.toString(), e);
                }
            }
        } finally {
            globalSync.writeUnlock();
        }
        log.debug("<< popOffWithRescan to height = " + height);
    }

    private int getBlockVersion(int previousBlockHeight) {

        return 3;
    }

    private int getTransactionVersion(int previousBlockHeight) {
        return 1;
    }



    public SortedSet<UnconfirmedTransaction> selectUnconfirmedTransactions(
            Map<TransactionType, Map<String, Integer>> duplicates, Block previousBlock, int blockTimestamp, int limit) {

        List<UnconfirmedTransaction> orderedUnconfirmedTransactions = new ArrayList<>();
        DbIterator<UnconfirmedTransaction> allUnconfirmedTransactions = lookupTransactionProcessor().getAllUnconfirmedTransactions();
        try (FilteringIterator<UnconfirmedTransaction> unconfirmedTransactions = new FilteringIterator<>(
                allUnconfirmedTransactions,
                transaction -> referencedTransactionService.hasAllReferencedTransactions(
                        transaction.getTransaction(), previousBlock.getHeight() + 1))) {
            for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                orderedUnconfirmedTransactions.add(unconfirmedTransaction);
            }
        }
        SortedSet<UnconfirmedTransaction> sortedTransactions = new TreeSet<>(transactionArrivalComparator);
        int payloadLength = 0;
        int maxPayloadLength = blockchainConfig.getCurrentConfig().getMaxPayloadLength();
        txSelectLoop:
        while (payloadLength <= maxPayloadLength && sortedTransactions.size() <= blockchainConfig.getCurrentConfig().getMaxNumberOfTransactions()) {
            int prevNumberOfNewTransactions = sortedTransactions.size();
            for (UnconfirmedTransaction unconfirmedTransaction : orderedUnconfirmedTransactions) {
                int transactionLength = unconfirmedTransaction.getTransaction().getFullSize();
                if (sortedTransactions.contains(unconfirmedTransaction) || payloadLength + transactionLength > maxPayloadLength) {
                    continue;
                }
                if (unconfirmedTransaction.getVersion() != getTransactionVersion(previousBlock.getHeight())) {
                    continue;
                }
                if (blockTimestamp > 0 && (unconfirmedTransaction.getTimestamp() > blockTimestamp + Constants.MAX_TIMEDRIFT
                        || unconfirmedTransaction.getExpiration() < blockTimestamp)) {
                    continue;
                }
                try {
                    transactionValidator.validate(unconfirmedTransaction.getTransaction());
                } catch (AplException.ValidationException e) {
                    continue;
                }
                if (unconfirmedTransaction.getTransaction().attachmentIsDuplicate(duplicates, true)) {
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
        return sortedTransactions;
    }


    private static final Comparator<UnconfirmedTransaction> transactionArrivalComparator = Comparator
            .comparingLong(UnconfirmedTransaction::getArrivalTimestamp)
            .thenComparingInt(UnconfirmedTransaction::getHeight)
            .thenComparingLong(UnconfirmedTransaction::getId);

    public SortedSet<UnconfirmedTransaction> getUnconfirmedTransactions(Block previousBlock, int blockTimestamp, int limit) {
        //TODo What is duplicates list for?
        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        List<Transaction> phasedTransactions = phasingPollService.getFinishingTransactions(lookupBlockhain().getHeight() + 1);
        phasedTransactions.addAll(phasingPollService.getFinishingTransactionsByTime(previousBlock.getTimestamp(), blockTimestamp));
        for (Transaction phasedTransaction : phasedTransactions) {
            try {
                transactionValidator.validate(phasedTransaction);
                phasedTransaction.attachmentIsDuplicate(duplicates, false); // pre-populate duplicates map
            } catch (AplException.ValidationException ignore) {
            }
        }
//        validate and insert in unconfirmed_transaction db table all waiting transaction
        lookupTransactionProcessor().processWaitingTransactions();
        SortedSet<UnconfirmedTransaction> sortedTransactions = selectUnconfirmedTransactions(duplicates, previousBlock, blockTimestamp, limit);
        return sortedTransactions;
    }


    public void generateBlock(byte[] keySeed, int blockTimestamp, int timeout, int blockVersion) throws BlockNotAcceptedException {

        Block previousBlock = lookupBlockhain().getLastBlock();
        SortedSet<UnconfirmedTransaction> sortedTransactions = getUnconfirmedTransactions(previousBlock, blockTimestamp, Integer.MAX_VALUE);
        List<Transaction> blockTransactions = new ArrayList<>();
        MessageDigest digest = Crypto.sha256();
        long totalAmountATM = 0;
        long totalFeeATM = 0;
        int payloadLength = 0;
        for (UnconfirmedTransaction unconfirmedTransaction : sortedTransactions) {
            Transaction transaction = unconfirmedTransaction.getTransaction();
            blockTransactions.add(transaction);
            digest.update(transaction.getBytes());
            totalAmountATM += transaction.getAmountATM();
            totalFeeATM += transaction.getFeeATM();
            payloadLength += transaction.getFullSize();
        }
        byte[] payloadHash = digest.digest();
        digest.update(previousBlock.getGenerationSignature());
        final byte[] publicKey = Crypto.getPublicKey(keySeed);
        byte[] generationSignature = digest.digest(publicKey);
        byte[] previousBlockHash = Crypto.sha256().digest(((BlockImpl)previousBlock).bytes());

        Block block = new BlockImpl(blockVersion, blockTimestamp, previousBlock.getId(), totalAmountATM, totalFeeATM, payloadLength,
                payloadHash, publicKey, generationSignature, previousBlockHash, timeout, blockTransactions, keySeed);

        try {
            pushBlock(block);
            blockEvent.select(literal(BlockEventType.BLOCK_GENERATED)).fire(block);
            log.debug("Account " + Long.toUnsignedString(block.getGeneratorId()) + " generated block " + block.getStringId()
                    + " at height " + block.getHeight() + " timestamp " + block.getTimestamp() + " fee " + ((float)block.getTotalFeeATM())/Constants.ONE_APL);
        } catch (TransactionNotAcceptedException e) {
            log.debug("Generate block failed: " + e.getMessage());
            lookupTransactionProcessor().processWaitingTransactions();
            Transaction transaction = e.getTransaction();
            log.debug("Removing invalid transaction: " + transaction.getStringId());
            globalSync.writeLock();
            try {
                lookupTransactionProcessor().removeUnconfirmedTransaction(transaction);
            } finally {
                globalSync.writeUnlock();
            }
            throw e;
        } catch (BlockNotAcceptedException e) {
            log.debug("Generate block failed: " + e.getMessage());
            throw e;
        }
    }


    void scheduleScan(int height, boolean validate) {
        TransactionalDataSource dataSource = lookupDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE scan SET rescan = TRUE, height = ?, validate = ?")) {
            pstmt.setInt(1, height);
            pstmt.setBoolean(2, validate);
            pstmt.executeUpdate();
            log.debug("Scheduled scan starting from height " + height + (validate ? ", with validation" : ""));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void scan(int height, boolean validate) {
        scan(height, validate, false);
    }

    @Override
    public void fullScanWithShutdown() {
        scan(0, true, true);
    }

    private void scan(int height, boolean validate, boolean shutdown) {
        log.debug(">> scan height={}, validate={}, shutdown={}", height, validate, shutdown);
        globalSync.writeLock();
        TransactionalDataSource dataSource = lookupDataSource();
        try {
            if (!dataSource.isInTransaction()) {
                try {
                    dataSource.begin();
                    scan(height, validate, shutdown);
                    dataSource.commit();
                } catch (Exception e) {
                    dataSource.rollback();
                    throw e;
                }
                return;
            }
            int shardInitialHeight = blockchain.getShardInitialBlock().getHeight();
            if (height < shardInitialHeight) {
                log.warn("Scanning of blocks that before the last shard block is not supported (height={} < shardInitialHeight={})", height, shardInitialHeight);
                return;
            }
            scheduleScan(height, validate);
            if (height > 0 && height < getMinRollbackHeight()) {
                log.info("Rollback to height less than {} (min rollback height) not supported, will do a full scan", getMinRollbackHeight());
                height = shardInitialHeight;
            }
            if (height < 0) {
                height = shardInitialHeight;
            }
            if (!shardImporter.canImport(height)) {
                log.info("Unable to import shard at height {}", height);
                return;
            }
            log.info("Scanning blockchain starting from height " + height + "...");
            if (validate) {
                log.debug("Also verifying signatures and validating transactions...");
            }

            String scanTaskId = aplAppStatus.durableTaskStart("Blockchain scan", "Rollback derived tables and scan blockchain blocks and transactions from given height to extract and save derived data", true);
            isScanning = true;
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmtSelect = con.prepareStatement("SELECT * FROM block WHERE " + (height > shardInitialHeight ? "height >= ? AND " : "")
                         + " db_id >= ? ORDER BY db_id ASC LIMIT 50000");
                 PreparedStatement pstmtDone = con.prepareStatement("UPDATE scan SET rescan = FALSE, height = 0, validate = FALSE")) {
                initialScanHeight = blockchain.getHeight();
                if (height > blockchain.getHeight() + 1) {
                    pstmtDone.executeUpdate();
                    dataSource.commit(false);
                    String message = "Rollback height " + (height - 1) + " exceeds current blockchain height of " + blockchain.getHeight() + ", no scan needed";
                    log.info(message);
                    return;
                }
                if (height == shardInitialHeight) {
                    trimService.resetTrim(height+trimService.getMaxRollback());
                    aplAppStatus.durableTaskUpdate(scanTaskId, 0.5, "Dropping all full text search indexes");
                    lookupFullTextSearchProvider().dropAll(con);
                    aplAppStatus.durableTaskUpdate(scanTaskId, 3.5, "Full text indexes dropped successfully");
                }
                Collection<DerivedTableInterface> derivedTables = dbTables.getDerivedTables();
                double percentsPerTable = getPercentsPerEvent(16.0, derivedTables.size());
                aplAppStatus.durableTaskUpdate(scanTaskId, 4.0, "Rollback " + derivedTables.size() + " tables");
                for (DerivedTableInterface table : derivedTables) {
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
                dataSource.clearCache();
                dataSource.commit(false);
                aplAppStatus.durableTaskUpdate(scanTaskId, 20.0, "Rolled back " + derivedTables.size() + " derived tables");
                Block currentBlock = blockchain.getBlockAtHeight(height);
                log.debug("scan currentBlock={} at height={}", currentBlock, height);
                blockEvent.select(literal(BlockEventType.RESCAN_BEGIN)).fire(currentBlock);
                long currentBlockId = currentBlock.getId();
                if (height == shardInitialHeight) {
                    blockchain.setLastBlock(currentBlock); // special case to avoid no last block
                    aplAppStatus.durableTaskUpdate(scanTaskId, 20.5, "Apply genesis");
                    shardImporter.importLastShard(height);
                    aplAppStatus.durableTaskUpdate(scanTaskId, 24.5, "Genesis applied");
                } else {
                    blockchain.setLastBlock(blockchain.getBlockAtHeight(height - 1));
                }
                lookupBlockhainConfigUpdater().rollback(blockchain.getLastBlock().getHeight());
                if (shutdown) {
                    log.info("Scan will be performed at next start");
                    new Thread(() -> System.exit(0)).start();
                    return;
                }
                int pstmtSelectIndex = 1;
                if (height > shardInitialHeight) {
                    pstmtSelect.setInt(pstmtSelectIndex++, height);
                }
                aplAppStatus.durableTaskUpdate(scanTaskId, 25.0, "Scanning blocks");

                int totalBlocksToScan = (blockchain.findLastBlock().getHeight() - height);
                double percentsPerThousandBlocks = getPercentsPerEvent(70.0, totalBlocksToScan / 1000);
                int blockCounter = 0;
                long dbId = Long.MIN_VALUE;
                boolean hasMore = true;
                outer:
                while (hasMore) {
                    hasMore = false;
                    pstmtSelect.setLong(pstmtSelectIndex, dbId);
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        while (rs.next()) {
                            try {
                                dbId = rs.getLong("db_id");
                                currentBlock = blockchain.loadBlock(con, rs, true);
                                if (currentBlock.getHeight() > shardInitialHeight) {
                                    ((BlockImpl)currentBlock).loadTransactions();
                                    if (currentBlock.getId() != currentBlockId || currentBlock.getHeight() > blockchain.getHeight() + 1) {
                                        throw new AplException.NotValidException("Database blocks in the wrong order!");
                                    }
                                    Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
                                    List<Transaction> validPhasedTransactions = new ArrayList<>();
                                    List<Transaction> invalidPhasedTransactions = new ArrayList<>();
                                    validatePhasedTransactions(currentBlock, blockchain.getLastBlock(), validPhasedTransactions, invalidPhasedTransactions, duplicates);
                                    if (validate && currentBlock.getHeight() > shardInitialHeight) {
                                        int curTime = timeService.getEpochTime();
                                        validator.validate(currentBlock, blockchain.getLastBlock(), curTime);
                                        byte[] blockBytes = ((BlockImpl)currentBlock).bytes();
                                        JSONObject blockJSON = (JSONObject) JSONValue.parse(currentBlock.getJSONObject().toJSONString());
                                        if (!Arrays.equals(blockBytes,
                                                BlockImpl.parseBlock(blockJSON).bytes())) {
                                            throw new AplException.NotValidException("Block JSON cannot be parsed back to the same block");
                                        }
                                        validateTransactions(currentBlock, blockchain.getLastBlock(), curTime, duplicates, true);
                                        for (Transaction transaction : currentBlock.getOrLoadTransactions()) {
                                            byte[] transactionBytes = ((TransactionImpl)transaction).bytes();
                                            if (!Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionBytes).build().bytes())) {
                                                throw new AplException.NotValidException("Transaction bytes cannot be parsed back to the same transaction: "
                                                        + transaction.getJSONObject().toJSONString());
                                            }
                                            JSONObject transactionJSON = (JSONObject) JSONValue.parse(transaction.getJSONObject().toJSONString());
                                            if (!Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionJSON).build().bytes())) {
                                                throw new AplException.NotValidException("Transaction JSON cannot be parsed back to the same transaction: "
                                                        + transaction.getJSONObject().toJSONString());
                                            }
                                        }
                                    }
                                    blockEvent.select(literal(BlockEventType.BEFORE_BLOCK_ACCEPT)).fire(currentBlock);
                                    blockchain.setLastBlock(currentBlock);
                                    accept(currentBlock, validPhasedTransactions, invalidPhasedTransactions, duplicates);
                                    dataSource.clearCache();
                                    dataSource.commit(false);
                                    blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(currentBlock);
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
                                        + (currentBlock == null ? 0 : currentBlock.getHeight()) + " failed, deleting from database");
                                Block lastBlock = blockchain.deleteBlocksFrom(currentBlockId);
                                blockchain.setLastBlock(lastBlock);
                                popOffTo(lastBlock);
                                break outer;
                            }
                            if (validate) {
                                blockEvent.select(literal(BlockEventType.BLOCK_SCANNED), new AnnotationLiteral<ScanValidate>() {}).fire(currentBlock);
                            } else {
                                blockEvent.select(literal(BlockEventType.BLOCK_SCANNED)).fire(currentBlock);
                            }
                            hasMore = true;
                        }
                        dbId = dbId + 1;
                    }
                }
                aplAppStatus.durableTaskUpdate(scanTaskId, 95.0, "All blocks scanned");
                double percentsPerTableIndex = getPercentsPerEvent(4.0, derivedTables.size());
                if (height == shardInitialHeight) {
                    for (DerivedTableInterface table : derivedTables) {
                        aplAppStatus.durableTaskUpdate(scanTaskId,
                                "Create full text search index for table " + table.toString(), percentsPerTableIndex);
                        table.createSearchIndex(con);
                    }
                }

                pstmtDone.executeUpdate();
                dataSource.commit(false);
                blockEvent.select(literal(BlockEventType.RESCAN_END)).fire(currentBlock);
                log.info("Scan done at height " + blockchain.getHeight());
                if (height == shardInitialHeight && validate) {
                    log.info("SUCCESSFULLY PERFORMED FULL RESCAN WITH VALIDATION");
                }
                lastRestoreTime = 0;
            } catch (SQLException e) {
                //if (e.getErrorCode() != 90007) { //The error with code 90007 is thrown when trying to call a JDBC method on an object that has been closed.
                try {
                    dataSource.rollback(false);
                }catch (IllegalStateException ex){
                    log.error("Error during the Rollback caused by SQL Exception", e);
                }
                //}
                throw new BlockchainScanException(e.toString(), e);
            } finally {
                isScanning = false;
                aplAppStatus.durableTaskFinished(scanTaskId, false, "");
            }
        } catch (Exception e) {
            // just for logging possible hidden error
            log.error("popOffWithRescan ->scan error", e);
        } finally {
            globalSync.writeUnlock();
        }
        log.debug("<< scan height={}, validate={}, shutdown={}", height, validate, shutdown);
    }

    private double getPercentsPerEvent(double totalPercents, int events) {
        return totalPercents / Math.max(events, 1);
    }

    @Override
    public void setGetMoreBlocks(boolean getMoreBlocks) {
        log.debug("Setting thread for block downloading into '{}'", getMoreBlocks);
        this.getMoreBlocks = getMoreBlocks;
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
    public void waitUntilBlockchainDownloadingStops(){
        log.debug("Waiting until blockchain downloading stops.");
        globalSync.updateLock();
        globalSync.updateUnlock();
    }

    private class GetMoreBlocksThread implements Runnable {

        public GetMoreBlocksThread() {
        }
        private final JSONStreamAware getCumulativeDifficultyRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getCumulativeDifficulty");
            request.put("chainId", blockchainConfig.getChain().getChainId());
            getCumulativeDifficultyRequest = JSON.prepareRequest(request);
        }
        private boolean peerHasMore;
        private List<Peer> connectedPublicPeers;
        private List<Long> chainBlockIds;
        private long totalTime = 1;
        private int totalBlocks;

        @Override
        public void run() {
            try {
                //
                // Download blocks until we are up-to-date
                //
                while (true) {
                    if (!getMoreBlocks) {
                        return;
                    }
                    int chainHeight = lookupBlockhain().getHeight();
                    downloadPeer();
                    log.trace("Is finished BCH download ? ({}), h1={}, h2={}, isDownloading={}",
                            lookupBlockhain().getHeight() == chainHeight, chainHeight,
                            lookupBlockhain().getHeight(), isDownloading);
                    if (lookupBlockhain().getHeight() == chainHeight) {
                        if (isDownloading) {
                            log.info("Finished blockchain download");
                            isDownloading = false;
                        }
                        break;
                    }
                }
                //
                // Restore prunable data
                //
                int now = timeService.getEpochTime();
                if (!isRestoring && !prunableTransactions.isEmpty() && now - lastRestoreTime > 60 * 60) {
                    isRestoring = true;
                    lastRestoreTime = now;
                    networkService.submit(new RestorePrunableDataTask());
                }
            } catch (InterruptedException e) {
                log.debug("Blockchain download thread interrupted");
            } catch (Throwable t) {
                log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
                System.exit(1);
            }
        }

        private void downloadPeer() throws InterruptedException {
            try {
                long startTime = System.currentTimeMillis();
                int numberOfForkConfirmations = lookupBlockhain().getHeight() > Constants.LAST_CHECKSUM_BLOCK - Constants.MAX_AUTO_ROLLBACK ?
                        defaultNumberOfForkConfirmations : Math.min(1, defaultNumberOfForkConfirmations);
                connectedPublicPeers = lookupPeersService().getPublicPeers(PeerState.CONNECTED, true);
                if (connectedPublicPeers.size() <= numberOfForkConfirmations) {
                    log.trace("downloadPeer connected = {} <= numberOfForkConfirmations = {}",
                            connectedPublicPeers.size(), numberOfForkConfirmations);
                    return;
                }
                peerHasMore = true;
                final Peer peer = lookupPeersService().getWeightedPeer(connectedPublicPeers);
                if (peer == null) {
                    log.debug("Can not find weighted peer");
                    return;
                }
                JSONObject response = peer.send(getCumulativeDifficultyRequest, blockchainConfig.getChain().getChainId());
                if (response == null) {
                    log.debug("Null response wile getCumulativeDifficultyRequest from peer {}",peer.getHostWithPort());
                    return;
                }
                Block lastBlock = lookupBlockhain().getLastBlock();
                BigInteger curCumulativeDifficulty = lastBlock.getCumulativeDifficulty();
                String peerCumulativeDifficulty = (String) response.get("cumulativeDifficulty");
                if (peerCumulativeDifficulty == null) {
                    return;
                }
                BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
                    return;
                }
                if (response.get("blockchainHeight") != null) {
                    lastBlockchainFeeder = peer;
                    //we can get sometimes string and sometimes long from different peers, and yes it's strange
                    Long bhl;
                    Object bh = response.get("blockchainHeight");
                    if (bh == null) {
                        bhl = 0L;
                    } else if (bh instanceof String) {
                       bhl = Long.parseLong((String)bh);
                    }else{
                       bhl = (Long)bh;
                    }
                    lastBlockchainFeederHeight = bhl.intValue();
                }
                if (betterCumulativeDifficulty.equals(curCumulativeDifficulty)) {
                    return;
                }
                
                long commonMilestoneBlockId = blockchain.getShardInitialBlock().getId();
                
                if (lookupBlockhain().getHeight() > 0) {
                    commonMilestoneBlockId = getCommonMilestoneBlockId(peer);
                }
                if (commonMilestoneBlockId == 0 || !peerHasMore) {
                    return;
                }
                
                chainBlockIds = getBlockIdsAfterCommon(peer, commonMilestoneBlockId, false);
                if (chainBlockIds.size() < 2 || !peerHasMore) {
                    if (commonMilestoneBlockId == blockchain.getShardInitialBlock().getId()) {
                        log.info("Cannot load blocks after genesis block {} from peer {}, perhaps using different Genesis block",
                                commonMilestoneBlockId, peer.getAnnouncedAddress());
                    }
                    return;
                }
                
                final long commonBlockId = chainBlockIds.get(0);
                final Block commonBlock = lookupBlockhain().getBlock(commonBlockId);
                if (commonBlock == null || lookupBlockhain().getHeight() - commonBlock.getHeight() >= Constants.MAX_AUTO_ROLLBACK) {
                    if (commonBlock != null) {
                        log.debug("Peer {} advertised chain with better difficulty, but the last common block is at height {}, peer info - {}", peer.getAnnouncedAddress(), commonBlock.getHeight(), peer);
                    }
                    return;
                }

                if (!isDownloading && lastBlockchainFeederHeight - commonBlock.getHeight() > 10) {
                    log.info("Blockchain download in progress");
                    isDownloading = true;
                }
//TODO: check do we need lock here
// Maybe better to find another sync solution
                globalSync.updateLock();
//                Generator.suspendForging();
                try {
                    if (betterCumulativeDifficulty.compareTo(lookupBlockhain().getLastBlock().getCumulativeDifficulty()) <= 0) {
                        return;
                    }
                    long lastBlockId = lookupBlockhain().getLastBlock().getId();
                    downloadBlockchain(peer, commonBlock, commonBlock.getHeight());
                    if (lookupBlockhain().getHeight() - commonBlock.getHeight() <= 10) {
                        return;
                    }
                    
                    int confirmations = 0;
                    for (Peer otherPeer : connectedPublicPeers) {
                        if (confirmations >= numberOfForkConfirmations) {
                            break;
                        }
                        if (peer.getHost().equals(otherPeer.getHost())) {
                            continue;
                        }
                        chainBlockIds = getBlockIdsAfterCommon(otherPeer, commonBlockId, true);
                        if (chainBlockIds.isEmpty()) {
                            continue;
                        }
                        long otherPeerCommonBlockId = chainBlockIds.get(0);
                        if (otherPeerCommonBlockId == lookupBlockhain().getLastBlock().getId()) {
                            confirmations++;
                            continue;
                        }
                        Block otherPeerCommonBlock = lookupBlockhain().getBlock(otherPeerCommonBlockId);
                        if (lookupBlockhain().getHeight() - otherPeerCommonBlock.getHeight() >= Constants.MAX_AUTO_ROLLBACK) {
                            continue;
                        }
                        String otherPeerCumulativeDifficulty;
                        JSONObject otherPeerResponse = peer.send(getCumulativeDifficultyRequest, blockchainConfig.getChain().getChainId());
                        if (otherPeerResponse == null || (otherPeerCumulativeDifficulty = (String) response.get("cumulativeDifficulty")) == null) {
                            continue;
                        }
                        if (new BigInteger(otherPeerCumulativeDifficulty).compareTo(lookupBlockhain().getLastBlock().getCumulativeDifficulty()) <= 0) {
                            continue;
                        }
                        log.debug("Found a peer with better difficulty: {}", otherPeer.getHostWithPort());
                        downloadBlockchain(otherPeer, otherPeerCommonBlock, commonBlock.getHeight());
                    }
                    log.debug("Got " + confirmations + " confirmations");
                    
                    if (lookupBlockhain().getLastBlock().getId() != lastBlockId) {
                        long time = System.currentTimeMillis() - startTime;
                        totalTime += time;
                        int numBlocks = lookupBlockhain().getHeight() - commonBlock.getHeight();
                        totalBlocks += numBlocks;
                        log.info("Downloaded " + numBlocks + " blocks in "
                                + time / 1000 + " s, " + (totalBlocks * 1000) / totalTime + " per s, "
                                + totalTime * (lastBlockchainFeederHeight - lookupBlockhain().getHeight()) / ((long) totalBlocks * 1000 * 60) + " min left");
                    } else {
                        log.debug("Did not accept peer's blocks, back to our own fork");
                    }
                } finally {
                    globalSync.updateUnlock();
                    isDownloading = false;
//                    Generator.resumeForging();
                }
            } catch (AplException.StopException e) {
                log.info("Blockchain download stopped: " + e.getMessage());
                throw new InterruptedException("Blockchain download stopped");
            } catch (Exception e) {
                log.info("Error in blockchain download thread", e);
            }
        }

        /**
         * Get first mutual block which peer has in blockchain and we have in blockchain
         * @param peer node which will supply us with possible mutual blocks
         * @return id of the first mutual block
         */
        private long getCommonMilestoneBlockId(Peer peer) {
            
            String lastMilestoneBlockId = null;
            
            while (true) {
                JSONObject milestoneBlockIdsRequest = new JSONObject();
                milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                milestoneBlockIdsRequest.put("chainId", blockchainConfig.getChain().getChainId());
                if (lastMilestoneBlockId == null) {
                    milestoneBlockIdsRequest.put("lastBlockId", lookupBlockhain().getLastBlock().getStringId());
                } else {
                    milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
                }
                
                JSONObject response;
                try {
                    response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest), blockchainConfig.getChain().getChainId());
                } catch (PeerNotConnectedException ex) {
                    response=null;
                }
                if (response == null) {
                    return 0;
                }
                JSONArray milestoneBlockIds = (JSONArray) response.get("milestoneBlockIds");
                if (milestoneBlockIds == null) {
                    return 0;
                }
                if (milestoneBlockIds.isEmpty()) {
                    return blockchain.getShardInitialBlock().getId();
                }
                // prevent overloading with blockIds
                if (milestoneBlockIds.size() > 20) {
                    log.debug("Obsolete or rogue peer " + peer.getHost() + " sends too many milestoneBlockIds, blacklisting");
                    peer.blacklist("Too many milestoneBlockIds");
                    return 0;
                }
                if (Boolean.TRUE.equals(response.get("last"))) {
                    peerHasMore = false;
                }
                for (Object milestoneBlockId : milestoneBlockIds) {
                    long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                    if (lookupBlockhain().hasBlock(blockId)) {
                        if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
                            peerHasMore = false;
                        }
                        return blockId;
                    }
                    lastMilestoneBlockId = (String) milestoneBlockId;
                }
            }
            
        }

        private List<Long> getBlockIdsAfterCommon(final Peer peer, final long startBlockId, final boolean countFromStart) {
            long matchId = startBlockId;
            List<Long> blockList = new ArrayList<>(Constants.MAX_AUTO_ROLLBACK);
            boolean matched = false;
            int limit = countFromStart ? Constants.MAX_AUTO_ROLLBACK : Constants.MAX_AUTO_ROLLBACK * 2;
            while (true) {
                JSONObject request = new JSONObject();
                request.put("requestType", "getNextBlockIds");
                request.put("blockId", Long.toUnsignedString(matchId));
                request.put("limit", limit);
                request.put("chainId", blockchainConfig.getChain().getChainId());
                JSONObject response;
                try {
                    response = peer.send(JSON.prepareRequest(request), blockchainConfig.getChain().getChainId());
                } catch (PeerNotConnectedException ex) {
                   response=null;
                }
                if (response == null) {
                    log.debug("null reaponse from peer {} while getNeBlockIdst",peer.getHostWithPort());
                    return Collections.emptyList();
                }
                JSONArray nextBlockIds = (JSONArray) response.get("nextBlockIds");
                if (nextBlockIds == null || nextBlockIds.size() == 0) {
                    break;
                }
                // prevent overloading with blockIds
                if (nextBlockIds.size() > limit) {
                    log.debug("Obsolete or rogue peer " + peer.getHost() + " sends too many nextBlockIds, blacklisting");
                    peer.blacklist("Too many nextBlockIds");
                    return Collections.emptyList();
                }
                boolean matching = true;
                int count = 0;
                for (Object nextBlockId : nextBlockIds) {
                    long blockId = Convert.parseUnsignedLong((String)nextBlockId);
                    if (matching) {
                        if (lookupBlockhain().hasBlock(blockId)) {
                            matchId = blockId;
                            matched = true;
                        } else {
                            blockList.add(matchId);
                            blockList.add(blockId);
                            matching = false;
                        }
                    } else {
                        blockList.add(blockId);
                        if (blockList.size() >= Constants.MAX_AUTO_ROLLBACK) {
                            break;
                        }
                    }
                    if (countFromStart && ++count >= Constants.MAX_AUTO_ROLLBACK) {
                        break;
                    }
                }
                if (!matching || countFromStart) {
                    break;
                }
            }
            if (blockList.isEmpty() && matched) {
                blockList.add(matchId);
            }
            return blockList;
        }

        /**
         * Download the block chain
         *
         * @param   feederPeer              Peer supplying the blocks list
         * @param   commonBlock             Common block
         * @throws  InterruptedException    Download interrupted
         */
        private void downloadBlockchain(final Peer feederPeer, final Block commonBlock, final int startHeight) throws InterruptedException {
            log.debug("Downloading blockchain from: {} at height: {}", feederPeer.getHostWithPort(), startHeight);
            Map<Long, PeerBlock> blockMap = new HashMap<>();
            //
            // Break the download into multiple segments.  The first block in each segment
            // is the common block for that segment.
            //
            List<GetNextBlocksTask> getList = new ArrayList<>();
            int segSize = Constants.MAX_AUTO_ROLLBACK / 20;
            int stop = chainBlockIds.size() - 1;
            for (int start = 0; start < stop; start += segSize) {
                getList.add(new GetNextBlocksTask(chainBlockIds, start, Math.min(start + segSize, stop), startHeight, blockchainConfig));
            }
            int nextPeerIndex = ThreadLocalRandom.current().nextInt(connectedPublicPeers.size());
            long maxResponseTime = 0;
            Peer slowestPeer = null;
            //
            // Issue the getNextBlocks requests and get the results.  We will repeat
            // a request if the peer didn't respond or returned a partial block list.
            // The download will be aborted if we are unable to get a segment after
            // retrying with different peers.
            //
            download: while (!getList.isEmpty()) {
                //
                // Submit threads to issue 'getNextBlocks' requests.  The first segment
                // will always be sent to the feeder peer.  Subsequent segments will
                // be sent to the feeder peer if we failed trying to download the blocks
                // from another peer.  We will stop the download and process any pending
                // blocks if we are unable to download a segment from the feeder peer.
                //
                for (GetNextBlocksTask nextBlocks : getList) {
                    Peer peer;
                    if (nextBlocks.getRequestCount() > 1) {
                        break download;
                    }
                    if (nextBlocks.getStart() == 0 || nextBlocks.getRequestCount() != 0) {
                        peer = feederPeer;
                    } else {
                        if (nextPeerIndex >= connectedPublicPeers.size()) {
                            nextPeerIndex = 0;
                        }
                        peer = connectedPublicPeers.get(nextPeerIndex++);
                    }
                    if (nextBlocks.getPeer() == peer) {
                        break download;
                    }
                    nextBlocks.setPeer(peer);
                    Future<List<BlockImpl>> future = networkService.submit(nextBlocks);
                    nextBlocks.setFuture(future);
                }
                //
                // Get the results.  A peer is on a different fork if a returned
                // block is not in the block identifier list.
                //
                Iterator<GetNextBlocksTask> it = getList.iterator();
                while (it.hasNext()) {
                    GetNextBlocksTask nextBlocks = it.next();
                    List<BlockImpl> blockList;
                    try {
                        blockList = nextBlocks.getFuture().get();
                    } catch (ExecutionException exc) {
                        throw new RuntimeException(exc.getMessage(), exc);
                    }
                    if (blockList == null) {
// most crtainly this is wrong. We should not kill peer if it does not have blocks higher then we
//                        nextBlocks.getPeer().deactivate();
                        continue;
                    }
                    Peer peer = nextBlocks.getPeer();
                    int index = nextBlocks.getStart() + 1;
                    for (Block block : blockList) {
                        if (block.getId() != chainBlockIds.get(index)) {
                            break;
                        }
                        blockMap.put(block.getId(), new PeerBlock(peer, block));
                        index++;
                    }
                    if (index > nextBlocks.getStop()) {
                        it.remove();
                    } else {
                        nextBlocks.setStart(index - 1);
                    }
                    if (nextBlocks.getResponseTime() > maxResponseTime) {
                        maxResponseTime = nextBlocks.getResponseTime();
                        slowestPeer = nextBlocks.getPeer();
                    }
                }
                
            }
            if (slowestPeer != null && connectedPublicPeers.size() >= PeersService.maxNumberOfConnectedPublicPeers && chainBlockIds.size() > Constants.MAX_AUTO_ROLLBACK / 2) {
                log.debug("Solwest peer {} took {} ms, disconnecting", slowestPeer.getHost(), maxResponseTime);
                slowestPeer.deactivate("This peer is slowest");
            }
            //
            // Add the new blocks to the blockchain.  We will stop if we encounter
            // a missing block (this will happen if an invalid block is encountered
            // when downloading the blocks)
            //
//TODO: check do we need this lock
// Maybe better to find another sync solution
            globalSync.writeLock();
            try {
                List<Block> forkBlocks = new ArrayList<>();
            for (int index = 1; index < chainBlockIds.size() && lookupBlockhain().getHeight() - startHeight < Constants.MAX_AUTO_ROLLBACK; index++) {
                    PeerBlock peerBlock = blockMap.get(chainBlockIds.get(index));
                    if (peerBlock == null) {
                        break;
                    }
                    Block block = peerBlock.getBlock();
                    if (lookupBlockhain().getLastBlock().getId() == block.getPreviousBlockId()) {
                        try {
                            pushBlock(block);
                        } catch (BlockNotAcceptedException e) {
                            peerBlock.getPeer().blacklist(e);
                        }
                    } else {
                        forkBlocks.add(block);
                    }
                }
                //
                // Process a fork
                //
                int myForkSize = lookupBlockhain().getHeight() - startHeight;
            if (!forkBlocks.isEmpty() && myForkSize < Constants.MAX_AUTO_ROLLBACK) {
                    log.debug("Will process a fork of {} blocks, mine is {}, feed peer addr: {}", forkBlocks.size(), myForkSize, feederPeer.getHost());
                    processFork(feederPeer, forkBlocks, commonBlock);
                }
            } finally {
                globalSync.writeUnlock();
            }
            
        }

        private void processFork(final Peer peer, final List<Block> forkBlocks, final Block commonBlock) {
            
            BigInteger curCumulativeDifficulty = lookupBlockhain().getLastBlock().getCumulativeDifficulty();
            
            List<Block> myPoppedOffBlocks = popOffTo(commonBlock);
            
            int pushedForkBlocks = 0;
            if (lookupBlockhain().getLastBlock().getId() == commonBlock.getId()) {
                for (Block block : forkBlocks) {
                    if (lookupBlockhain().getLastBlock().getId() == block.getPreviousBlockId()) {
                        try {
                            pushBlock(block);
                            pushedForkBlocks += 1;
                        } catch (BlockNotAcceptedException e) {
                            peer.blacklist(e);
                            break;
                        }
                    }
                }
            }
            
            if (pushedForkBlocks > 0 && lookupBlockhain().getLastBlock().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {
                log.debug("Pop off caused by peer {}, blacklisting",peer.getHost());
                peer.blacklist("Pop off");
                List<Block> peerPoppedOffBlocks = popOffTo(commonBlock);
                pushedForkBlocks = 0;
                for (Block block : peerPoppedOffBlocks) {
                    lookupTransactionProcessor().processLater(block.getOrLoadTransactions());
                }
            }
            
            if (pushedForkBlocks == 0) {
                log.debug("Didn't accept any blocks, pushing back my previous blocks");
                for (int i = myPoppedOffBlocks.size() - 1; i >= 0; i--) {
                    Block block = myPoppedOffBlocks.remove(i);
                    try {
                        pushBlock(block);
                    } catch (BlockNotAcceptedException e) {
                        log.error("Popped off block no longer acceptable: " + block.getJSONObject().toJSONString(), e);
                        break;
                    }
                }
            } else {
                log.debug("Switched to peer's fork, peer addr: {}",peer.getHost());
                for (Block block : myPoppedOffBlocks) {
                    lookupTransactionProcessor().processLater(block.getOrLoadTransactions());
                }
            }
            
        }
    }

}