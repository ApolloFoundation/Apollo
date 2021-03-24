/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.MemPoolUnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
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
    private final IteratorToStreamConverter<UnconfirmedTransactionEntity> streamConverter = new IteratorToStreamConverter<>();
    private final MemPoolUnconfirmedTransactionTable table;
    private final MemPoolInMemoryState memoryState;
    private final GlobalSync globalSync;
    private final TransactionValidator validator;
    private final UnconfirmedTransactionEntityToModelConverter toModelConverter;
    private final UnconfirmedTransactionModelToEntityConverter toEntityConverter;
    private final boolean enableRebroadcasting;
    private final int maxUnconfirmedTransactions;
    private final int maxCachedTransactions;

    @Inject
    public MemPool(MemPoolUnconfirmedTransactionTable table,
                   MemPoolInMemoryState memoryState,
                   GlobalSync globalSync,
                   TransactionValidator validator,
                   UnconfirmedTransactionEntityToModelConverter toModelConverter,
                   UnconfirmedTransactionModelToEntityConverter toEntityConverter,
                   @Property(name = "apl.maxUnconfirmedTransactions", defaultValue = "" + Integer.MAX_VALUE) int maxUnconfirmedTransactions,
                   @Property(name = "apl.mempool.maxCachedTransactions", defaultValue = "2000") int maxCachedTransactions,
                   @Property(name = "apl.enableTransactionRebroadcasting") boolean enableRebroadcasting) {
        this.table = table;
        this.maxUnconfirmedTransactions = maxUnconfirmedTransactions;
        this.maxCachedTransactions = maxCachedTransactions;
        this.enableRebroadcasting = enableRebroadcasting;
        this.memoryState = memoryState;
        this.globalSync = globalSync;
        this.validator = validator;
        this.toModelConverter = toModelConverter;
        this.toEntityConverter = toEntityConverter;
    }

    public void initCache(){
        // Initialize the unconfirmed transaction cache if it hasn't been done yet
        if (!memoryState.isCacheInitialized()) {
            memoryState.initializeCache(streamConverter.apply(table.getAll(0, -1)).map(toModelConverter));
        }
    }

    public Transaction getUnconfirmedTransaction(long id) {
        Transaction transaction = memoryState.getFromCache(id);
        if (transaction != null) {
            return transaction;
        }
        return toModelConverter.convert(table.getById(id));
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
        return memoryState.getFromCache(exclude);
    }

    public void addToBroadcastedTransactions(Transaction tx) {
        memoryState.addToBroadcasted(tx);
    }

    public void broadcastLater(Transaction tx) {
        memoryState.broadcastLater(tx);
    }

    public boolean addProcessed(UnconfirmedTransaction tx) {
        boolean canSaveTxs = getUnconfirmedTxCount() < maxUnconfirmedTransactions;
        if (canSaveTxs) {
            table.insert(
                toEntityConverter.convert(tx)
            );
            memoryState.putInCache(tx);
        }
        return canSaveTxs;
    }

    public Stream<UnconfirmedTransaction> getAllProcessedStream() {
        return table.getAllUnconfirmedTransactionsStream().map(toModelConverter);
    }

    public int getUnconfirmedTxCount() {
        if(getCachedUnconfirmedTxCount() < maxCachedTransactions){
            return getCachedUnconfirmedTxCount();
        } else {
            return table.getCount();
        }
    }

    public int getCachedUnconfirmedTxCount() {
        return memoryState.txCacheSize();
    }

    public void removeBroadcastedTransaction(Transaction transaction) {
        memoryState.removeBroadcasted(List.of(transaction));
    }

    public boolean canSafelyAcceptTransactions(int numTx) {
        return canSafelyAccept() - numTx >= 0;
    }

    public int canSafelyAccept() {
        return maxUnconfirmedTransactions - getUnconfirmedTxCount();
    }

    public List<Transaction> allPendingTransactions() {
        return memoryState.allPendingTransactions();
    }

    public Stream<UnconfirmedTransaction> getProcessed(int from, int to) {
        return streamConverter.convert(table.getAll(from, to)).map(toModelConverter);
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
                memoryState.addToBroadcasted(e.getTransactionImpl());
            }
        });
    }

    public boolean removeProcessedTransaction(long id) {
        boolean deleted = table.deleteById(id);
        memoryState.removeFromCache(id);
        return deleted;
    }

    public void rebroadcast(Transaction tx) {
        if (enableRebroadcasting) {
            memoryState.addToBroadcasted(tx);
        }
    }

    public List<Long> getAllProcessedIds() {
        if(getCachedUnconfirmedTxCount() < maxCachedTransactions){
            return memoryState.getAllUnconfirmedTransactionIds();
        } else {
            return table.getAllUnconfirmedTransactionIds();
        }
    }

    public int countExpiredTxs(int epochTime) {
        return table.countExpiredTransactions(epochTime);
    }


    public Stream<UnconfirmedTransaction> getExpiredTxsStream(int epochTime) {
        return table.getExpiredTxsStream(epochTime).map(toModelConverter);
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
