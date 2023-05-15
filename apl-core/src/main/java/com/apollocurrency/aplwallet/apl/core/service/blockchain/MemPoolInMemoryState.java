/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.model.WrappedTransaction;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.SizeBoundedPriorityQueue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;

@Singleton
public class MemPoolInMemoryState {
    private static final Comparator<UnconfirmedTransaction> cachedUnconfirmedTransactionComparator =
        comparingInt(UnconfirmedTransaction::getHeight) // Sort by transaction_height ASC
            .thenComparing(comparingLong(UnconfirmedTransaction::getFeePerByte).reversed()) // Sort by fee_per_byte DESC
            .thenComparingLong(UnconfirmedTransaction::getArrivalTimestamp) // Sort by arrival_timestamp ASC
            .thenComparingLong(UnconfirmedTransaction::getId); // Sort by transaction ID ASC

    private final Map<Long, UnconfirmedTransaction> transactionCache = new ConcurrentHashMap<>();
    private final Map<Transaction, Transaction> txToBroadcastWhenConfirmed = new ConcurrentHashMap<>();
    private final Set<Transaction> broadcastedTransactions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicInteger referencedCounter = new AtomicInteger(-1);
    private final AtomicBoolean cacheInitialized = new AtomicBoolean(false);

    private final PriorityBlockingQueue<UnconfirmedTransaction> processLaterQueue;
    private final IdQueue<UnconfirmedTransaction> pendingProcessingQueue;
    private final MemPoolConfig memPoolConfig;


    @Inject
    public MemPoolInMemoryState(MemPoolConfig memPoolConfig) {
        this.processLaterQueue = new SizeBoundedPriorityQueue<>(memPoolConfig.getProcessLaterQueueSize(), new UnconfirmedTransactionComparator());
        this.pendingProcessingQueue = new IdQueue<>(new ArrayDeque<>(), WrappedTransaction::getId, memPoolConfig.getMaxPendingTransactions());
        this.memPoolConfig = memPoolConfig;
    }

    public void processLater(UnconfirmedTransaction unconfirmedTransaction) {
        processLaterQueue.add(unconfirmedTransaction);
    }

    public int pendingProcessingSize() {
        return pendingProcessingQueue.size();
    }

    public UnconfirmedTransaction nextPendingProcessing() {
        return pendingProcessingQueue.remove();
    }

    public boolean pendingProcessingContains(long id) {
        return pendingProcessingQueue.contains(id);
    }

    public int pendingProcessingReminingCapacity() {
        return memPoolConfig.getMaxPendingTransactions() - pendingProcessingQueue.size();
    }

    public Iterator<UnconfirmedTransaction> processLaterIterator() {
        return processLaterQueue.iterator();
    }

    public boolean addToBroadcasted(Transaction transaction) {
        return broadcastedTransactions.add(transaction);
    }

    public Collection<Transaction> getAllBroadcasted() {
        return new ArrayList<>(broadcastedTransactions);
    }

    public void addTxToBroadcastWhenConfirmed(Transaction tx, Transaction unconfirmedTransaction) {
        txToBroadcastWhenConfirmed.put(tx, unconfirmedTransaction);
    }

    public void putInCache(UnconfirmedTransaction unconfirmedTransaction) {
        if (transactionCache.size() < memPoolConfig.getMaxCachedTransactions()) {
            transactionCache.put(unconfirmedTransaction.getId(), unconfirmedTransaction);
        }
        if (unconfirmedTransaction.getReferencedTransactionFullHash() != null) {
            referencedCounter.incrementAndGet();
        }
    }

    public IdQueue.ReturnCode addPendingProcessing(UnconfirmedTransaction unconfirmedTransaction) {
        return pendingProcessingQueue.addWithStatus(unconfirmedTransaction);
    }

    public void initializeCache(Stream<UnconfirmedTransaction> unconfirmedTransactionStream) {
        if(cacheInitialized.compareAndSet(false, true)){
            AtomicInteger referencedCount = new AtomicInteger(0);
            CollectionUtil.forEach(unconfirmedTransactionStream, e -> {
                transactionCache.put(e.getId(), e);
                if (e.getReferencedTransactionFullHash() != null) {
                    referencedCount.incrementAndGet();
                }
            });
            referencedCounter.set(referencedCount.get());
        } else {
            unconfirmedTransactionStream.close();
        }
    }

    public Set<UnconfirmedTransaction> getFromCache(List<String> exclude) {
        TreeSet<UnconfirmedTransaction> sortedUnconfirmedTransactions = new TreeSet<>(cachedUnconfirmedTransactionComparator);
        transactionCache.values().forEach(transaction -> {
            if (Collections.binarySearch(exclude, transaction.getStringId()) < 0) {
                sortedUnconfirmedTransactions.add(transaction);
            }
        });
        return sortedUnconfirmedTransactions;
    }

    public List<Long> getAllCachedIds(){
        return new ArrayList<>(transactionCache.keySet());
    }

    public void clear() {
        transactionCache.clear();
        txToBroadcastWhenConfirmed.clear();
        broadcastedTransactions.clear();
        processLaterQueue.clear();
        referencedCounter.set(0);
        pendingProcessingQueue.clear();
    }

    public List<UnconfirmedTransaction> getAllPendingProcessing() {
        return new ArrayList<>(pendingProcessingQueue);
    }

    public int txCacheSize() {
        return transactionCache.size();
    }

    public UnconfirmedTransaction getFromCache(long id) {
        return transactionCache.get(id);
    }

    public int referencedRemainingCapacity() {
        return Math.max(0, memPoolConfig.getMaxReferencedTxs() - referencedCounter.get());
    }

    public int getReferencedCount() {
        return referencedCounter.get();
    }

    public void removeCached(Transaction transaction) {
        transactionCache.remove(transaction.getId());
        if (transaction.getReferencedTransactionFullHash() != null) {
            referencedCounter.decrementAndGet();
        }
    }

    public boolean isBroadcasted(Transaction transaction) {
        return broadcastedTransactions.contains(transaction);
    }

    public void removeBroadcasted(List<Transaction> transactions) {
        broadcastedTransactions.removeAll(transactions);
    }

    public Map<Transaction, Transaction> getAllBroadcastWhenConfirmed() {
        return new HashMap<>(txToBroadcastWhenConfirmed);
    }

    public void removeBroadcastedWhenConfirmed(Collection<Transaction> transactions) {
        transactions.forEach(txToBroadcastWhenConfirmed::remove);
    }

    public int processLaterSize() {
        return processLaterQueue.size();
    }

    public boolean isCacheInitialized(){
        return cacheInitialized.get();
    }

    public void removePendingProcessing(long id) {
        pendingProcessingQueue.remove(id);
    }
}
