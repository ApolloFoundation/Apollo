/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.MemPoolUnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class MemPool {


    private final IteratorToStreamConverter<UnconfirmedTransaction> streamConverter = new IteratorToStreamConverter<>();;
    private final MemPoolUnconfirmedTransactionTable table;
    private final MemPoolInMemoryState memoryState;
    private final GlobalSync globalSync;
    private final TransactionValidator validator;
    private final boolean enableRebroadcasting;
    private final int maxUnconfirmedTransactions;

    @Inject
    public MemPool(MemPoolUnconfirmedTransactionTable table,
                   MemPoolInMemoryState memoryState,
                   GlobalSync globalSync,
                   TransactionValidator validator,
                   @Property(name = "apl.maxUnconfirmedTransactions", defaultValue = "" + Integer.MAX_VALUE) int maxUnconfirmedTransactions,
                   @Property(name = "apl.enableTransactionRebroadcasting") boolean enableRebroadcasting) {
        this.table = table;
        this.maxUnconfirmedTransactions = maxUnconfirmedTransactions;
        this.enableRebroadcasting = enableRebroadcasting;
        this.memoryState = memoryState;
        this.globalSync = globalSync;
        this.validator = validator;
    }

    public Transaction getUnconfirmedTransaction(long id) {
        Transaction transaction = memoryState.getFromCache(id);
        if (transaction != null) {
            return transaction;
        }
        return table.getById(id);
    }

    public boolean hasUnconfirmedTransaction(long id) {
        return getUnconfirmedTransaction(id) != null;
    }

    public Collection<Transaction> getAllBroadcastedTransactions() {
        globalSync.readLock();
        try {
            return memoryState.getAllBroadcastedTransactions();
        } finally {
            globalSync.readUnlock();
        }
    }

    public void broadcastWhenConfirmed(Transaction tx, Transaction unconfirmedTx) {
        memoryState.addTxToBroadcastWhenConfirmed(tx, unconfirmedTx);
    }

    public Set<UnconfirmedTransaction> getCachedUnconfirmedTransactions(List<String> exclude) {
        //
        // Initialize the unconfirmed transaction cache if it hasn't been done yet
        //
        if (!memoryState.isCacheInitialized()) {
            memoryState.initializeCache(streamConverter.apply(table.getAll(0, -1)));
        }
        return memoryState.getFromCache(exclude);
    }

    public void addToBroadcastedTransactions(Transaction tx) {
        memoryState.addToBroadcasted(tx);
    }

    public void broadcastLater(Transaction tx) {
        memoryState.broadcastLater(tx);
    }

    public boolean addProcessed(UnconfirmedTransaction tx) {
        boolean canSaveTxs = allProcessedCount() < maxUnconfirmedTransactions;
        if (canSaveTxs) {
            table.insert(tx);
            memoryState.putInCache(tx);
        }
        return canSaveTxs;
    }



    public Stream<UnconfirmedTransaction> getAllProcessedStream() {
        return table.getAllUnconfirmedTransactionsStream();
    }

    public int allProcessedCount() {
        return table.getCount();
    }

    public void removeBroadcastedTransaction(Transaction transaction) {
        memoryState.removeBroadcasted(List.of(transaction));
    }

    public boolean canSafelyAcceptTransactions(int numTx) {
        return canSafelyAccept() - numTx >= 0;
    }

    public int canSafelyAccept() {
        return maxUnconfirmedTransactions - allProcessedCount();
    }

    public List<Transaction> allPendingTransactions() {
        return memoryState.allPendingTransactions();
    }

    public boolean isProcessedTxPoolFull() {
        return allProcessedCount() >= maxUnconfirmedTransactions;
    }

    public List<UnconfirmedTransaction> getProcessed(int from, int to) {
        return CollectionUtil.toList(table.getAll(from, to));
    }

    public void processLater(UnconfirmedTransaction unconfirmedTransaction) {
        memoryState.processLater(unconfirmedTransaction);
    }

    public Iterator<UnconfirmedTransaction> processLaterQueueIterator() {
        return memoryState.processLaterQueueIterator();
    }

    public int processLaterQueueSize() {
        return memoryState.processLaterQueueSize();
    }

    public boolean softBroadcast(Transaction uncTx) throws AplException.ValidationException {
        validator.validateLightly(uncTx);
        return memoryState.addToSoftBroadcastingQueue(uncTx);
    }

    public void clear() {
        memoryState.clear();
        table.truncate();
    }

    public Transaction nextSoftBroadcastTransaction() throws InterruptedException {
        return memoryState.nextBroadcastPendingTransaction();
    }

    public void rebroadcastAllUnconfirmedTransactions() {
        getAllProcessedStream().forEach(e -> {
            if (enableRebroadcasting) {
                memoryState.addToBroadcasted(e.getTransaction());
            }
        });
    }

    public int currentCacheSize() {
        return memoryState.txCacheSize();
    }

    public boolean removeProcessedTransaction(long id) {
        boolean deleted = table.deleteById(id);
        memoryState.removeFromCache(id);
        return deleted;
    }

    public void rebroadcast(Transaction tx) {
        if (enableRebroadcasting) {
            memoryState.addToBroadcasted(tx);
//            log.debug("Transaction {} already in unconfirmed pool, will re-broadcast", tx.getStringId());
        } else {
//            log.debug("Transaction {} already in unconfirmed pool, will not broadcast again", tx.getStringId());
        }
    }

    public List<Long> getAllProcessedIds() {
        return table.getAllUnconfirmedTransactionIds();
    }

    public int countExpiredTxs(int epochTime) {
        return table.countExpiredTransactions(epochTime);
    }


    public Stream<UnconfirmedTransaction> getExpiredTxsStream(int epochTime) {
        return table.getExpiredTxsStream(epochTime);
    }

    public boolean isAlreadyBroadcasted(Transaction transaction) {
        return memoryState.isBroadcasted(transaction);
    }

    public void removeFromBroadcasted(List<Transaction> transactions) {
        memoryState.removeBroadcasted(transactions);
    }

    public Map<Transaction, Transaction> getAllBroadcastWhenConfirmedTransactions() {
        return memoryState.getAllBroadcastWhenConfirmedTransactions();
    }

    public void removeBroadcastedWhenConfirmedTransaction(Collection<Transaction> transactions) {
        memoryState.removeBroadcastedWhenConfirmedTransactions(transactions);
    }

    public int pendingBroadcastQueueSize() {
        return memoryState.pendingBroadcastQueueSize();
    }

    public double pendingBroadcastQueueLoad() {
        return memoryState.pendingBroadcastQueueLoadFactor();
    }
}
