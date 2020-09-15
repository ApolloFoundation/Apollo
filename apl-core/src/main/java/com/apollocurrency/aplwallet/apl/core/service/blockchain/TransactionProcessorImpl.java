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

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTxsToBroadcastWhenConfirmed;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessWaitingTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RebroadcastTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RemoveUnconfirmedTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class TransactionProcessorImpl implements TransactionProcessor {



    private final TransactionValidator transactionValidator;
    private final TransactionBuilder transactionBuilder;
    private final PropertiesHolder propertiesHolder;
    private final PrunableLoadingService prunableService;
    private final BlockchainConfig blockchainConfig;
    private final NtpTime ntpTime;
    private final Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private final TimeService timeService;
    private final GlobalSync globalSync;
    private final javax.enterprise.event.Event<List<Transaction>> txsEvent;
    private final TaskDispatchManager taskDispatchManager;
    private final PeersService peers;
    private final MemPool memPool;
    private final DatabaseManager databaseManager;
    private final UnconfirmedTransactionProcessingService processingService;

    private final RebroadcastTransactionsThread rebroadcastTransactionsThread;
    private final ProcessWaitingTransactionsThread processWaitingTransactionsThread;
    private final ProcessTransactionsThread processTransactionsThread;
    private final RemoveUnconfirmedTransactionsThread removeUnconfirmedTransactionsThread;
    private final ProcessTxsToBroadcastWhenConfirmed processTxsToBroadcastWhenConfirmed;

    @Inject
    public TransactionProcessorImpl(PropertiesHolder propertiesHolder,
                                    TransactionValidator validator,
                                    Event<List<Transaction>> txEvent,
                                    DatabaseManager databaseManager,
                                    GlobalSync globalSync,
                                    TimeService timeService,
                                    NtpTime ntpTime,
                                    BlockchainConfig blockchainConfig,
                                    TaskDispatchManager taskDispatchManager,
                                    PeersService peers,
                                    Blockchain blockchain, TransactionBuilder transactionBuilder,
                                    PrunableLoadingService prunableService, UnconfirmedTransactionProcessingService processingService, TransactionTypeFactory transactionTypeFactory,
                                    MemPool memPool) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.transactionValidator = validator;
        this.txsEvent = Objects.requireNonNull(txEvent);
        this.databaseManager = databaseManager;
        this.globalSync = globalSync;
        this.timeService = Objects.requireNonNull(timeService);
        this.ntpTime = ntpTime;
        this.blockchainConfig = blockchainConfig;
        this.taskDispatchManager = taskDispatchManager;
        this.peers = Objects.requireNonNull(peers);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.transactionBuilder = transactionBuilder;
        this.prunableService = prunableService;
        this.processingService = processingService;
        // threads creation
        this.rebroadcastTransactionsThread = new RebroadcastTransactionsThread(
            this.timeService, memPool, this.peers, this.blockchain);
        this.processWaitingTransactionsThread = new ProcessWaitingTransactionsThread(this);
        this.processTransactionsThread = new ProcessTransactionsThread(
            this, memPool, blockchainConfig, peers,
            transactionTypeFactory);
        this.removeUnconfirmedTransactionsThread = new RemoveUnconfirmedTransactionsThread(
            this.databaseManager, this, this.timeService, memPool, this.globalSync);
        this.processTxsToBroadcastWhenConfirmed = new ProcessTxsToBroadcastWhenConfirmed(
            memPool, this.timeService, this.blockchain);
        this.memPool = memPool;
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
        return blockchainProcessor;
    }

    public void init() {
        configureBackgroundTasks();
    }

    private void configureBackgroundTasks() {
        if (!propertiesHolder.isLightClient()) {
            TaskDispatcher dispatcher = taskDispatchManager.newBackgroundDispatcher("TransactionProcessorService");
            if (!propertiesHolder.isOffline()) {
                dispatcher.schedule(Task.builder()
                    .name("ProcessTransactions")
                    .delay(1000)
                    .task(processTransactionsThread)
                    .build());
                dispatcher.schedule(Task.builder()
                    .name("MemPoolChecker")
                    .delay(1000)
                    .task(() -> {
                        printMemPoolStat();
                    })
                    .build());
                dispatcher.invokeAfter(Task.builder()
                    .name("Broadcaster")
                    .initialDelay(2000)
                    .task(this::broadcastPendingQueue)
                    .build());
                dispatcher.invokeAfter(Task.builder()
                    .name("InitialUnconfirmedTxsRebroadcasting")
                    .task(this::rebroadcastAllUnconfirmedTransactions)
                    .build());

                dispatcher.schedule(Task.builder()
                    .name("RebroadcastTransactions")
                    .delay(8000)
                    .task(rebroadcastTransactionsThread)
                    .build());
            }
            dispatcher.schedule(Task.builder()
                .name("RemoveUnconfirmedTransactions")
                .delay(7000)
                .task(removeUnconfirmedTransactionsThread)
                .build());
            dispatcher.schedule(Task.builder()
                .name("ProcessWaitingTransactions")
                .delay(333)
                .task(processWaitingTransactionsThread)
                .build());
            dispatcher.schedule(Task.builder()
                .name("ProcessTransactionsToBroadcastWhenConfirmed")
                .delay(15000)
                .task(processTxsToBroadcastWhenConfirmed)
                .build());
        }
    }

    @Override
    public void broadcastWhenConfirmed(Transaction transaction, Transaction uncTransaction) {
        memPool.broadcastWhenConfirmed(transaction, uncTransaction);
    }

    void broadcastPendingQueue() {
        while (true) {
            if (memPool.pendingBroadcastQueueSize() > 10) {
                globalSync.updateLock();
                try {
                    for (int i = 0; i < 10; i++) {
                        doBroadcastOnePendingTx();
                    }
                } finally {
                    globalSync.updateUnlock();
                }
                ThreadUtils.sleep(20);
            } else {
                doBroadcastOnePendingTx();
                ThreadUtils.sleep(5);
            }
        }
    }

    private void doBroadcastOnePendingTx() {
        if (!memPool.canSafelyAcceptTransactions()) { // do not loose existing transactions
            return;
        }
        try {
            Transaction tx = null;
            try {
                tx = memPool.nextSoftBroadcastTransaction();
            } catch (InterruptedException ignored) {}
            if (tx != null) {
                try {
                    transactionValidator.validate(tx);
                } catch (AplException.ValidationException e) {
                    log.debug("Invalid transaction was not broadcasted txId=" + tx.getId(), e);
                    return;
                }
                UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(tx);
                if (!validationResult.isOk()) {
                    return;
                }
                try {
                    broadcast(tx);
                } catch (AplException.ValidationException e) {
                    log.debug("Failed to broadcast transaction txId=" + tx.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Unknown exception during broadcast transactions ", e);
        }
    }

    @Override
    public void printMemPoolStat() {
        log.trace("Processed: {}, waiting {}, pending broadcast - {}", memPool.allProcessedCount(), memPool.getWaitingTransactionsQueueSize(), memPool.pendingBroadcastQueueSize());
    }

    @Override
    public void broadcast(Transaction transaction) throws AplException.ValidationException {
        globalSync.writeLock();
        try {
            if (blockchain.hasTransaction(transaction.getId())) {
                log.info("Transaction {} already in blockchain, will not broadcast again", transaction.getStringId());
                return;
            }
            if (memPool.hasUnconfirmedTransaction(transaction.getId())) {
                memPool.rebroadcast(transaction);
                return;
            }
            transactionValidator.validate(transaction);
            UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, ntpTime.getTime());
            boolean broadcastLater = lookupBlockchainProcessor().isProcessingBlock();
            if (broadcastLater) {
                memPool.broadcastLater(transaction);
//                log.debug("Will broadcast new transaction later {}", transaction.getStringId());
            } else {
                UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(unconfirmedTransaction);
                if (!validationResult.isOk()) {
                    throw new AplException.NotValidException(validationResult.getErrorDescription());
                }
                processingService.processTransaction(unconfirmedTransaction);
//                log.debug("Accepted new transaction {}", transaction.getStringId());
                List<Transaction> acceptedTransactions = Collections.singletonList(unconfirmedTransaction);
                peers.sendToSomePeers(acceptedTransactions);
                txsEvent.select(TxEventType.literal(TxEventType.ADDED_UNCONFIRMED_TRANSACTIONS)).fire(acceptedTransactions);
                memPool.rebroadcast(transaction);
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public void clearUnconfirmedTransactions() {
        globalSync.writeLock();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        List<UnconfirmedTransaction> transactions;
        try {
            dataSource.beginTransactionIfNotStarted();
            transactions = processingService.undoAllProcessedTransactions();
            memPool.clear();
            dataSource.commit();
        } catch (Exception e) {
            log.error(e.toString(), e);
            dataSource.rollback();
            throw e;
        } finally {
            globalSync.writeUnlock();
        }
        log.trace("Unc txs cleared");
        List<Transaction> removedTxs = transactions.stream().map(UnconfirmedTransaction::getTransaction).collect(Collectors.toList());
        txsEvent.select(TxEventType.literal(TxEventType.REMOVED_UNCONFIRMED_TRANSACTIONS)).fire(removedTxs);
    }

    @Override
    public void requeueAllUnconfirmedTransactions() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        globalSync.writeLock();
        TransactionalDataSource.StartedConnection startedConnection = dataSource.beginTransactionIfNotStarted();
        try {

            List<Transaction> removed = new ArrayList<>();
            List<UnconfirmedTransaction> transactions = processingService.undoAllProcessedTransactions();
            transactions.forEach(memPool::addToWaitingQueue);
            memPool.resetProcessedState();
            txsEvent.select(TxEventType.literal(TxEventType.REMOVED_UNCONFIRMED_TRANSACTIONS)).fire(removed);
            dataSource.commit(!startedConnection.isAlreadyStarted());
        } catch (Exception e) {
            log.error(e.toString(), e);
            dataSource.rollback(!startedConnection.isAlreadyStarted());
            throw e;
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public void rebroadcastAllUnconfirmedTransactions() {
        memPool.rebroadcastAllUnconfirmedTransactions();
    }

    public void removeUnconfirmedTransaction(Transaction transaction) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            dataSource.beginTransactionIfNotStarted();
            boolean removed = memPool.removeProcessedTransaction(transaction.getId());
            if (removed) {
//                log.trace("Removing unc tx {}", transaction.getId());
                processingService.undoProcessedTransaction(transaction);
                txsEvent.select(TxEventType.literal(TxEventType.REMOVED_UNCONFIRMED_TRANSACTIONS)).fire(Collections.singletonList(transaction));
            }
            dataSource.commit();
        } catch (Exception e) {
            dataSource.rollback();
            log.error(e.toString(), e);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void processLater(Collection<Transaction> transactions) {
        long currentTime = ntpTime.getTime();
        globalSync.writeLock();
        try {
            for (Transaction transaction : transactions) {
//                blockchain.getTransactionCache().remove(transaction.getId());
                if (blockchain.hasTransaction(transaction.getId())) {
                    continue;
                }
//                log.trace("Process later tx {}", transaction.getId());
                transaction.unsetBlock();
                memPool.addToWaitingQueue(new UnconfirmedTransaction(
                    transaction, Math.min(currentTime, Convert2.fromEpochTime(transaction.getTimestamp())))
                );
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    public void processWaitingTransactions() {
        globalSync.writeLock();
        try {
            if (memPool.getWaitingTransactionsQueueSize() > 0) {
                int currentTime = timeService.getEpochTime();
                List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
                Iterator<UnconfirmedTransaction> iterator =
                    memPool.getWaitingTransactionsQueueIterator();
                while (iterator.hasNext()) {
                    UnconfirmedTransaction unconfirmedTransaction = iterator.next();
                    try {
//                        log.trace("Process waiting tx {}", unconfirmedTransaction.getId());
                        transactionValidator.validate(unconfirmedTransaction);
                        UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(unconfirmedTransaction);
                        if (!validationResult.isOk()) {
                            processValidationResult(validationResult, unconfirmedTransaction, iterator);
                        } else {
                            processingService.processTransaction(unconfirmedTransaction);
                            iterator.remove();
                            addedUnconfirmedTransactions.add(unconfirmedTransaction.getTransaction());
                        }
                    } catch (AplException.ExistingTransactionException e) {
                        iterator.remove();
//                        log.trace("Tx processing error " + unconfirmedTransaction.getId(), e);
                    } catch (AplException.NotCurrentlyValidException e) {
                        if (unconfirmedTransaction.getExpiration() < currentTime
                            || currentTime - Convert2.toEpochTime(unconfirmedTransaction.getArrivalTimestamp()) > 3600) {
                            iterator.remove();
                        }
//                        log.trace("Tx is not valid currently " + unconfirmedTransaction.getId(), e);
                    } catch (AplException.ValidationException | RuntimeException e) {
                        iterator.remove();
//                        log.trace("Validation tx processing error " + unconfirmedTransaction.getId(), e);
                    }
                }
                if (addedUnconfirmedTransactions.size() > 0) {
                    txsEvent.select(TxEventType.literal(TxEventType.ADDED_UNCONFIRMED_TRANSACTIONS)).fire(addedUnconfirmedTransactions);
                }
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    private void processValidationResult(UnconfirmedTxValidationResult result, UnconfirmedTransaction tx, Iterator<UnconfirmedTransaction> iterator) {
        long currentTime = timeService.getEpochTime();
        if (result.getError() == UnconfirmedTxValidationResult.Error.ALREADY_PROCESSED || result.getError() == UnconfirmedTxValidationResult.Error.NOT_VALID) {
            iterator.remove();
        }
        if (result.getError() == UnconfirmedTxValidationResult.Error.NOT_CURRENTLY_VALID) {
            if (tx.getExpiration() < currentTime || currentTime - Convert2.toEpochTime(tx.getArrivalTimestamp()) > 3600) {
                iterator.remove();
            }
        }
    }

    @Override
    public int getWaitingTransactionsQueueSize(){
        return memPool.getWaitingTransactionsQueueSize();
    }

    @Override
    public int getUnconfirmedTxCount() {
        return memPool.allProcessedCount();
    }

    public void processPeerTransactions(List<Transaction> transactions) throws AplException.NotValidException {
        if (blockchain.getHeight() <= blockchainConfig.getLastKnownBlock()) {
            return;
        }
        if (CollectionUtil.isEmpty(transactions)) {
            return;
        }
        long arrivalTimestamp = ntpTime.getTime();
        List<Transaction> receivedTransactions = new ArrayList<>();
        List<Transaction> sendToPeersTransactions = new ArrayList<>();
        List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            try {
                receivedTransactions.add(transaction);
                if (memPool.hasUnconfirmedTransaction(transaction.getId()) || blockchain.hasTransaction(transaction.getId())) {
                    continue;
                }
                transactionValidator.validate(transaction);
                UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, arrivalTimestamp);
                globalSync.writeLock();
                try {
                    UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(unconfirmedTransaction);
                    if (validationResult.isOk()) {
                        processingService.processTransaction(unconfirmedTransaction);
                    } else if (validationResult.getError() == UnconfirmedTxValidationResult.Error.NOT_VALID) {
                        exceptions.add(new AplException.NotValidException(validationResult.getErrorDescription()));
                        continue;
                    }
                } finally {
                    globalSync.writeUnlock();
                }
                if (memPool.isAlreadyBroadcasted(transaction)) {
//                    log.debug("Received back transaction " + transaction.getStringId()
//                        + " that we broadcasted, will not forward again to peers");
                } else {
                    sendToPeersTransactions.add(unconfirmedTransaction);
                }
                addedUnconfirmedTransactions.add(transaction);

            } catch (AplException.NotCurrentlyValidException ignore) {
            } catch (AplException.ValidationException | RuntimeException e) {
                log.warn(String.format("Invalid transaction from peer: %s", JSONData.unconfirmedTransaction(transaction)), e);
                exceptions.add(e);
            }
        }
        if (!sendToPeersTransactions.isEmpty()) {
            peers.sendToSomePeers(sendToPeersTransactions);
        }
        if (!addedUnconfirmedTransactions.isEmpty()) {
            txsEvent.select(TxEventType.literal(TxEventType.ADDED_UNCONFIRMED_TRANSACTIONS)).fire(addedUnconfirmedTransactions);
        }
        memPool.removeFromBroadcasted(receivedTransactions);
        if (!exceptions.isEmpty()) {
            throw new AplException.NotValidException("Peer sends invalid transactions: " + exceptions.toString());
        }
    }

    /**
     * Restore expired prunable data
     *
     * @param transactions Transactions containing prunable data
     * @return Processed transactions
     * @throws AplException.NotValidException Transaction is not valid
     */
    @Transactional(readOnly = true)
    @Override
    public List<Transaction> restorePrunableData(JSONArray transactions) throws AplException.NotValidException {
        List<Transaction> processed = new ArrayList<>();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        globalSync.readLock();
        try {
            dataSource.begin();
            try {
                //
                // Check each transaction returned by the archive peer
                //
                for (Object transactionJSON : transactions) {
                    Transaction transaction = parseTransaction((JSONObject) transactionJSON);
                    Transaction myTransaction = blockchain.findTransactionByFullHash(transaction.getFullHash());
                    if (myTransaction != null) {
                        boolean foundAllData = true;
                        //
                        // Process each prunable appendage
                        //
                        appendageLoop:
                        for (Appendix appendage : transaction.getAppendages()) {
                            if ((appendage instanceof Prunable)) {
                                //
                                // Don't load the prunable data if we already have the data
                                //
                                for (Appendix myAppendage : myTransaction.getAppendages()) {
                                    if (myAppendage.getClass() == appendage.getClass()) {
                                        prunableService.loadPrunable(myTransaction, myAppendage, true);
                                        if (((Prunable) myAppendage).hasPrunableData()) {
                                            if (log.isDebugEnabled()) {
                                                log.debug(String.format("Already have prunable data for transaction %s %s appendage",
                                                    myTransaction.getStringId(), myAppendage.getAppendixName()));
                                            }
                                            continue appendageLoop;
                                        }
                                        break;
                                    }
                                }
                                //
                                // Load the prunable data
                                //
                                if (((Prunable) appendage).hasPrunableData()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Loading prunable data for transaction {} {} appendage",
                                            Long.toUnsignedString(transaction.getId()), appendage.getAppendixName());
                                    }
                                    prunableService.restorePrunable(transaction, appendage, myTransaction.getBlockTimestamp(), myTransaction.getHeight());
                                } else {
                                    foundAllData = false;
                                }
                            }
                        }
                        if (foundAllData) {
                            processed.add(myTransaction);
                        }
                        dataSource.commit(false);
                    }
                }
                dataSource.commit();
            } catch (Exception e) {
                dataSource.rollback();
                processed.clear();
                throw e;
            }
        } finally {
            globalSync.readUnlock();
        }
        return processed;
    }



    public TransactionImpl parseTransaction(JSONObject transactionData) throws AplException.NotValidException {
        TransactionImpl transaction = transactionBuilder.newTransactionBuilder(transactionData).build();
        if (transaction.getSignature() != null && !transactionValidator.checkSignature(transaction)) {
            throw new AplException.NotValidException("Invalid transaction signature for transaction " + transaction.getId());
        }
        return transaction;
    }
}
