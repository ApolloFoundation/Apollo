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

import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessTxsToBroadcastWhenConfirmed;
import com.apollocurrency.aplwallet.apl.core.app.runnable.ProcessWaitingTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RebroadcastTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.app.runnable.RemoveUnconfirmedTransactionsThread;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TxEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;

@Slf4j
@Singleton
public class TransactionProcessorImpl implements TransactionProcessor {

    private static final Comparator<UnconfirmedTransaction> cachedUnconfirmedTransactionComparator =
        comparingInt(UnconfirmedTransaction::getHeight) // Sort by transaction_height ASC
            .thenComparing(comparingLong(UnconfirmedTransaction::getFeePerByte).reversed()) // Sort by fee_per_byte DESC
            .thenComparingLong(UnconfirmedTransaction::getArrivalTimestamp) // Sort by arrival_timestamp ASC
            .thenComparingLong(UnconfirmedTransaction::getId); // Sort by transaction ID ASC

    private final UnconfirmedTransactionTable unconfirmedTransactionTable;
    private final TransactionValidator transactionValidator;
    private final TransactionApplier transactionApplier;
    private final PropertiesHolder propertiesHolder;
    private final BlockchainConfig blockchainConfig;
    private final NtpTime ntpTime;
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private final TimeService timeService;
    private final GlobalSync globalSync;
    private final javax.enterprise.event.Event<List<Transaction>> txsEvent;
    private DatabaseManager databaseManager;
    private final TaskDispatchManager taskDispatchManager;
    private final PeersService peers;
    private final AccountService accountService;

    private final boolean enableTransactionRebroadcasting;
    private int maxUnconfirmedTransactions;
    private volatile boolean cacheInitialized = false;

    private RebroadcastTransactionsThread rebroadcastTransactionsThread;
    private ProcessWaitingTransactionsThread processWaitingTransactionsThread;
    private ProcessTransactionsThread processTransactionsThread;
    private RemoveUnconfirmedTransactionsThread removeUnconfirmedTransactionsThread;
    private ProcessTxsToBroadcastWhenConfirmed processTxsToBroadcastWhenConfirmed;

    @Inject
    public TransactionProcessorImpl(PropertiesHolder propertiesHolder,
                                    TransactionValidator validator,
                                    TransactionApplier applier,
                                    javax.enterprise.event.Event<List<Transaction>> txEvent,
                                    UnconfirmedTransactionTable unconfirmedTransactionTable,
                                    DatabaseManager databaseManager,
                                    AccountService accountService,
                                    GlobalSync globalSync,
                                    TimeService timeService,
                                    NtpTime ntpTime,
                                    BlockchainConfig blockchainConfig,
                                    TaskDispatchManager taskDispatchManager,
                                    PeersService peers,
                                    Blockchain blockchain) {
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.enableTransactionRebroadcasting = propertiesHolder.getBooleanProperty("apl.enableTransactionRebroadcasting");
        this.unconfirmedTransactionTable = Objects.requireNonNull(unconfirmedTransactionTable);
        this.validator = validator;
        this.transactionApplier = applier;
        this.txsEvent = Objects.requireNonNull(txEvent);
        this.databaseManager = databaseManager;
        this.accountService = accountService;
        this.globalSync = globalSync;
        this.timeService = Objects.requireNonNull(timeService);
        this.ntpTime = ntpTime;
        this.blockchainConfig = blockchainConfig;
        this.taskDispatchManager = taskDispatchManager;
        this.peers = Objects.requireNonNull(peers);
        this.blockchain = Objects.requireNonNull(blockchain);
        int n = propertiesHolder.getIntProperty("apl.maxUnconfirmedTransactions");
        this.maxUnconfirmedTransactions = n <= 0 ? Integer.MAX_VALUE : n;
        // threads creation
        this.rebroadcastTransactionsThread = new RebroadcastTransactionsThread(
            this.timeService, this.unconfirmedTransactionTable, this.peers, this.blockchain);
        this.processWaitingTransactionsThread = new ProcessWaitingTransactionsThread(this);
        this.processTransactionsThread = new ProcessTransactionsThread(
            this, this.unconfirmedTransactionTable, blockchainConfig, peers);
        this.removeUnconfirmedTransactionsThread = new RemoveUnconfirmedTransactionsThread(
            this.databaseManager, this.unconfirmedTransactionTable, this, this.timeService, this.globalSync);
        this.processTxsToBroadcastWhenConfirmed = new ProcessTxsToBroadcastWhenConfirmed(
            this, this.unconfirmedTransactionTable, this.timeService, this.blockchain);
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
                    .delay(5000)
                    .task(processTransactionsThread)
                    .build());
                dispatcher.invokeAfter(Task.builder()
                    .name("InitialUnconfirmedTxsRebroadcasting")
                    .task(this::rebroadcastAllUnconfirmedTransactions)
                    .build());

                dispatcher.schedule(Task.builder()
                    .name("RebroadcastTransactions")
                    .delay(23000)
                    .task(rebroadcastTransactionsThread)
                    .build());
            }
            dispatcher.schedule(Task.builder()
                .name("RemoveUnconfirmedTransactions")
                .delay(20000)
                .task(removeUnconfirmedTransactionsThread)
                .build());
            dispatcher.schedule(Task.builder()
                .name("ProcessWaitingTransactions")
                .delay(1000)
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
    public DbIterator<UnconfirmedTransaction> getAllUnconfirmedTransactions() {
        return unconfirmedTransactionTable.getAll(0, -1);
    }

    @Override
    public DbIterator<UnconfirmedTransaction> getAllUnconfirmedTransactions(int from, int to) {
        return unconfirmedTransactionTable.getAll(from, to);
    }

    @Override
    public DbIterator<UnconfirmedTransaction> getAllUnconfirmedTransactions(String sort) {
        return unconfirmedTransactionTable.getAll(0, -1, sort);
    }

    @Override
    public DbIterator<UnconfirmedTransaction> getAllUnconfirmedTransactions(int from, int to, String sort) {
        return unconfirmedTransactionTable.getAll(from, to, sort);
    }

    @Override
    public Transaction getUnconfirmedTransaction(long transactionId) {
        DbKey dbKey = unconfirmedTransactionTable.getTransactionKeyFactory().newKey(transactionId);
        return getUnconfirmedTransaction(dbKey);
    }

    Transaction getUnconfirmedTransaction(DbKey dbKey) {
        globalSync.readLock();
        try {
            Transaction transaction = unconfirmedTransactionTable.getTransactionCache().get(dbKey);
            if (transaction != null) {
                return transaction;
            }
        } finally {
            globalSync.readUnlock();
        }
        return unconfirmedTransactionTable.get(dbKey);
    }

    @Override
    public UnconfirmedTransaction[] getAllWaitingTransactions() {
        UnconfirmedTransaction[] transactions;
        globalSync.readLock();
        try {
            transactions = unconfirmedTransactionTable.getWaitingTransactionsCache().toArray(
                new UnconfirmedTransaction[unconfirmedTransactionTable.getWaitingTransactionsCacheSize()]);
        } finally {
            globalSync.readUnlock();
        }
        Arrays.sort(transactions, unconfirmedTransactionTable.getWaitingTransactionsCache().comparator());
        return transactions;
    }

    public Collection<UnconfirmedTransaction> getWaitingTransactions() {
        return unconfirmedTransactionTable.getWaitingTransactionsUnmodifiedCollection();
    }

    @Override
    public Transaction[] getAllBroadcastedTransactions() {
        globalSync.readLock();
        try {
            return unconfirmedTransactionTable.getBroadcastedTransactions().toArray(
                new TransactionImpl[unconfirmedTransactionTable.getBroadcastedTransactionsSize()]);
        } finally {
            globalSync.readUnlock();
        }
    }

    @Override
    public void broadcast(Transaction transaction) throws AplException.ValidationException {
        globalSync.writeLock();
        try {
            if (blockchain.hasTransaction(transaction.getId())) {
                log.info("Transaction {} already in blockchain, will not broadcast again", transaction.getStringId());
                return;
            }
            DbKey dbKey = unconfirmedTransactionTable.getTransactionKeyFactory().newKey(transaction.getId());
            if (getUnconfirmedTransaction(dbKey) != null) {
                if (enableTransactionRebroadcasting) {
                    unconfirmedTransactionTable.getBroadcastedTransactions().add(transaction);
                    log.info("Transaction {} already in unconfirmed pool, will re-broadcast", transaction.getStringId());
                } else {
                    log.info("Transaction {} already in unconfirmed pool, will not broadcast again", transaction.getStringId());
                }
                return;
            }
            transactionValidator.validate(transaction);
            UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, ntpTime.getTime());
            boolean broadcastLater = lookupBlockchainProcessor().isProcessingBlock();
            if (broadcastLater) {
                unconfirmedTransactionTable.getWaitingTransactionsCache().add(unconfirmedTransaction);
                unconfirmedTransactionTable.getBroadcastedTransactions().add(transaction);
                log.debug("Will broadcast new transaction later {}", transaction.getStringId());
            } else {
                processTransaction(unconfirmedTransaction);
                log.debug("Accepted new transaction {}", transaction.getStringId());
                List<Transaction> acceptedTransactions = Collections.singletonList(transaction);
                peers.sendToSomePeers(acceptedTransactions);
                txsEvent.select(TxEventType.literal(TxEventType.ADDED_UNCONFIRMED_TRANSACTIONS)).fire(acceptedTransactions);
                if (enableTransactionRebroadcasting) {
                    unconfirmedTransactionTable.getBroadcastedTransactions().add(transaction);
                }
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public void processPeerTransactions(JSONObject request) throws AplException.ValidationException {
        JSONArray transactionsData = (JSONArray) request.get("transactions");
        processPeerTransactions(transactionsData);
    }

    @Override
    public void clearUnconfirmedTransactions() {
        globalSync.writeLock();
        try {
            List<Transaction> removed = new ArrayList<>();
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try {
                dataSource.begin();
                try (DbIterator<UnconfirmedTransaction> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
                    for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                        transactionApplier.undoUnconfirmed(unconfirmedTransaction.getTransaction());
                        removed.add(unconfirmedTransaction.getTransaction());
                    }
                }
                unconfirmedTransactionTable.truncate();
                dataSource.commit();
            } catch (Exception e) {
                log.error(e.toString(), e);
                dataSource.rollback();
                throw e;
            }
            unconfirmedTransactionTable.getUnconfirmedDuplicates().clear();
            unconfirmedTransactionTable.getWaitingTransactionsCache().clear();
            unconfirmedTransactionTable.getBroadcastedTransactions().clear();
            unconfirmedTransactionTable.getTransactionCache().clear();
            txsEvent.select(TxEventType.literal(TxEventType.REMOVED_UNCONFIRMED_TRANSACTIONS)).fire(removed);
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public void requeueAllUnconfirmedTransactions() {
        globalSync.writeLock();
        try {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            if (!dataSource.isInTransaction()) {
                try {
                    dataSource.begin();
                    requeueAllUnconfirmedTransactions();
                    dataSource.commit();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                    dataSource.rollback();
                    throw e;
                }
                return;
            }
            List<Transaction> removed = new ArrayList<>();
            try (DbIterator<UnconfirmedTransaction> unconfirmedTransactions = getAllUnconfirmedTransactions()) {
                for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                    transactionApplier.undoUnconfirmed(unconfirmedTransaction.getTransaction());
                    if (removed.size() < maxUnconfirmedTransactions) {
                        removed.add(unconfirmedTransaction.getTransaction());
                    }
                    unconfirmedTransactionTable.getWaitingTransactionsCache().add(unconfirmedTransaction);
                }
            }
            unconfirmedTransactionTable.truncate();
            unconfirmedTransactionTable.getUnconfirmedDuplicates().clear();
            unconfirmedTransactionTable.getTransactionCache().clear();
            txsEvent.select(TxEventType.literal(TxEventType.REMOVED_UNCONFIRMED_TRANSACTIONS)).fire(removed);
        } finally {
            globalSync.writeUnlock();
        }
    }

    @Override
    public void rebroadcastAllUnconfirmedTransactions() {
        globalSync.writeLock();
        try {
            try (DbIterator<UnconfirmedTransaction> oldNonBroadcastedTransactions = getAllUnconfirmedTransactions()) {
                for (UnconfirmedTransaction unconfirmedTransaction : oldNonBroadcastedTransactions) {
                    if (unconfirmedTransaction.getTransaction().isUnconfirmedDuplicate(
                        unconfirmedTransactionTable.getUnconfirmedDuplicates())) {
                        log.debug("Skipping duplicate unconfirmed transaction " + unconfirmedTransaction.getTransaction().getJSONObject().toString());
                    } else if (enableTransactionRebroadcasting) {
                        unconfirmedTransactionTable.getBroadcastedTransactions().add(unconfirmedTransaction.getTransaction());
                    }
                }
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    public void removeUnconfirmedTransaction(Transaction transaction) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            try {
                dataSource.begin();
                removeUnconfirmedTransaction(transaction);
                dataSource.commit();
            } catch (Exception e) {
                log.error(e.toString(), e);
                dataSource.rollback();
                throw e;
            }
            return;
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM unconfirmed_transaction WHERE id = ?")) {
            pstmt.setLong(1, transaction.getId());
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                transactionApplier.undoUnconfirmed(transaction);
                DbKey dbKey = unconfirmedTransactionTable.getTransactionKeyFactory().newKey(transaction.getId());
                unconfirmedTransactionTable.getTransactionCache().remove(dbKey);
                txsEvent.select(TxEventType.literal(TxEventType.REMOVED_UNCONFIRMED_TRANSACTIONS)).fire(Collections.singletonList(transaction));
            }
        } catch (SQLException e) {
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
                transaction.unsetBlock();
                unconfirmedTransactionTable.getWaitingTransactionsCache().add(new UnconfirmedTransaction(
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
            if (unconfirmedTransactionTable.getWaitingTransactionsCacheSize() > 0) {
                int currentTime = timeService.getEpochTime();
                List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
                Iterator<UnconfirmedTransaction> iterator =
                    unconfirmedTransactionTable.getWaitingTransactionsCache().iterator();
                while (iterator.hasNext()) {
                    UnconfirmedTransaction unconfirmedTransaction = iterator.next();
                    try {
                        transactionValidator.validate(unconfirmedTransaction);
                        processTransaction(unconfirmedTransaction);
                        iterator.remove();
                        addedUnconfirmedTransactions.add(unconfirmedTransaction.getTransaction());
                    } catch (AplException.ExistingTransactionException e) {
                        iterator.remove();
                    } catch (AplException.NotCurrentlyValidException e) {
                        if (unconfirmedTransaction.getExpiration() < currentTime
                            || currentTime - Convert2.toEpochTime(unconfirmedTransaction.getArrivalTimestamp()) > 3600) {
                            iterator.remove();
                        }
                    } catch (AplException.ValidationException | RuntimeException e) {
                        iterator.remove();
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

    public void processPeerTransactions(JSONArray transactionsData) throws AplException.NotValidException {
        if (blockchain.getHeight() <= blockchainConfig.getLastKnownBlock()) {
            return;
        }
        if (transactionsData == null || transactionsData.isEmpty()) {
            return;
        }
        long arrivalTimestamp = ntpTime.getTime();
        List<Transaction> receivedTransactions = new ArrayList<>();
        List<Transaction> sendToPeersTransactions = new ArrayList<>();
        List<Transaction> addedUnconfirmedTransactions = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        for (Object transactionData : transactionsData) {
            try {
                Transaction transaction = TransactionImpl.parseTransaction((JSONObject) transactionData);
                receivedTransactions.add(transaction);
                DbKey dbKey = unconfirmedTransactionTable.getTransactionKeyFactory().newKey(transaction.getId());
                if (getUnconfirmedTransaction(dbKey) != null || blockchain.hasTransaction(transaction.getId())) {
                    continue;
                }
                transactionValidator.validate(transaction);
                UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(transaction, arrivalTimestamp);
                processTransaction(unconfirmedTransaction);
                if (unconfirmedTransactionTable.getBroadcastedTransactions().contains(transaction)) {
                    log.debug("Received back transaction " + transaction.getStringId()
                        + " that we broadcasted, will not forward again to peers");
                } else {
                    sendToPeersTransactions.add(transaction);
                }
                addedUnconfirmedTransactions.add(transaction);

            } catch (AplException.NotCurrentlyValidException ignore) {
            } catch (AplException.ValidationException | RuntimeException e) {
                log.debug(String.format("Invalid transaction from peer: %s", ((JSONObject) transactionData).toJSONString()), e);
                exceptions.add(e);
            }
        }
        if (sendToPeersTransactions.size() > 0) {
            peers.sendToSomePeers(sendToPeersTransactions);
        }
        if (addedUnconfirmedTransactions.size() > 0) {
            txsEvent.select(TxEventType.literal(TxEventType.ADDED_UNCONFIRMED_TRANSACTIONS)).fire(addedUnconfirmedTransactions);
        }
        unconfirmedTransactionTable.getBroadcastedTransactions().removeAll(receivedTransactions);
        if (!exceptions.isEmpty()) {
            throw new AplException.NotValidException("Peer sends invalid transactions: " + exceptions.toString());
        }
    }

    public void processTransaction(UnconfirmedTransaction unconfirmedTransaction) throws AplException.ValidationException {
        Transaction transaction = unconfirmedTransaction.getTransaction();
        int curTime = timeService.getEpochTime();
        if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT || transaction.getExpiration() < curTime) {
            throw new AplException.NotCurrentlyValidException("Invalid transaction timestamp");
        }
        if (transaction.getVersion() < 1) {
            throw new AplException.NotValidException("Invalid transaction version");
        }
        if (transaction.getId() == 0L) {
            throw new AplException.NotValidException("Invalid transaction id 0");
        }

        globalSync.writeLock();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            try {
                dataSource.begin();
                if (blockchain.getHeight() < blockchainConfig.getLastKnownBlock()) {
                    throw new AplException.NotCurrentlyValidException("Blockchain not ready to accept transactions");
                }
                DbKey dbKey = unconfirmedTransactionTable.getTransactionKeyFactory().newKey(transaction.getId());
                if (getUnconfirmedTransaction(dbKey) != null || blockchain.hasTransaction(transaction.getId())) {
                    throw new AplException.ExistingTransactionException("Transaction already processed");
                }

                if (!transactionValidator.verifySignature(transaction)) {
                    if (lookupAccountService().getAccount(transaction.getSenderId()) != null) {
                        throw new AplException.NotValidException("Transaction signature verification failed");
                    } else {
                        throw new AplException.NotCurrentlyValidException("Unknown transaction sender");
                    }
                }

                if (!transactionApplier.applyUnconfirmed(transaction)) {
                    throw new AplException.InsufficientBalanceException("Insufficient balance");
                }

                if (transaction.isUnconfirmedDuplicate(unconfirmedTransactionTable.getUnconfirmedDuplicates())) {
                    throw new AplException.NotCurrentlyValidException("Duplicate unconfirmed transaction");
                }

                unconfirmedTransactionTable.insert(unconfirmedTransaction);

                dataSource.commit();
            } catch (Exception e) {
                dataSource.rollback();
                throw e;
            }
        } finally {
            globalSync.writeUnlock();
        }
    }

    /**
     * Get the cached unconfirmed transactions
     *
     * @param exclude List of transaction identifiers to exclude
     */
    @Override
    public SortedSet<? extends Transaction> getCachedUnconfirmedTransactions(List<String> exclude) {
        SortedSet<UnconfirmedTransaction> transactionSet = new TreeSet<>(cachedUnconfirmedTransactionComparator);
        globalSync.readLock();
        try {
            //
            // Initialize the unconfirmed transaction cache if it hasn't been done yet
            //
            synchronized (unconfirmedTransactionTable.getTransactionCache()) {
                if (!cacheInitialized) {
                    DbIterator<UnconfirmedTransaction> it = getAllUnconfirmedTransactions();
                    while (it.hasNext()) {
                        UnconfirmedTransaction unconfirmedTransaction = it.next();
                        DbKey dbKey = unconfirmedTransactionTable.getTransactionKeyFactory().newKey(unconfirmedTransaction.getId());
                        unconfirmedTransactionTable.getTransactionCache().put(dbKey, unconfirmedTransaction);
                    }
                    cacheInitialized = true;
                }
            }
            //
            // Build the result set
            //
            unconfirmedTransactionTable.getTransactionCache().values().forEach(transaction -> {
                if (Collections.binarySearch(exclude, transaction.getStringId()) < 0) {
                    transactionSet.add(transaction);
                }
            });
        } finally {
            globalSync.readUnlock();
        }
        return transactionSet;
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
                    Transaction transaction = TransactionImpl.parseTransaction((JSONObject) transactionJSON);
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
                                        myAppendage.loadPrunable(myTransaction, true);
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
                                    ((Prunable) appendage).restorePrunableData(transaction, myTransaction.getBlockTimestamp(), myTransaction.getHeight());
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

    public void broadcastWhenConfirmed(Transaction tx, Transaction unconfirmedTx) {
        unconfirmedTransactionTable.getTxToBroadcastWhenConfirmed().put(tx, unconfirmedTx);
    }
}
