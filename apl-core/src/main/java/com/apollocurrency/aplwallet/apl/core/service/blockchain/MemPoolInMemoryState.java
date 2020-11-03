/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.util.SizeBoundedPriorityQueue;
import lombok.Data;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    private final PriorityBlockingQueue<TxWithArrivalTimestamp> broadcastPendingTransactions;
    private final PriorityBlockingQueue<UnconfirmedTransaction> processLaterQueue;

    @Getter
    private volatile boolean cacheInitialized;
    private final int maxPendingBroadcastQueueSize;
    private final int maxCachedTransactions;


    @Inject
    public MemPoolInMemoryState(@Property(name = "apl.mempool.maxPendingTransactions", defaultValue = "3000") int maxPendingTransactions,
                                @Property(name = "apl.mempool.maxCachedTransactions", defaultValue = "2000") int maxCachedTransactions,
                                @Property(name = "apl.mempool.processLaterQueueSize", defaultValue = "5000") int processLaterQueueSize
    ) {
        this.maxCachedTransactions = maxCachedTransactions;
        this.maxPendingBroadcastQueueSize = maxPendingTransactions;
        this.processLaterQueue = new SizeBoundedPriorityQueue<>(processLaterQueueSize, new UnconfirmedTransactionComparator());
        this.broadcastPendingTransactions = new PriorityBlockingQueue<>(maxPendingBroadcastQueueSize, Comparator.comparing(TxWithArrivalTimestamp::getArrivalTime)) {
            @Override
            public boolean offer(TxWithArrivalTimestamp txWithArrivalTimestamp) {
                if (size() == maxPendingBroadcastQueueSize) {
                    return false;
                }
                return super.offer(txWithArrivalTimestamp);
            }
        };
    }

    public void processLater(UnconfirmedTransaction unconfirmedTransaction) {
        processLaterQueue.add(unconfirmedTransaction);
    }

    public Iterator<UnconfirmedTransaction> processLaterQueueIterator() {
        return processLaterQueue.iterator();
    }

    public boolean addToSoftBroadcastingQueue(Transaction transaction) {
        return broadcastPendingTransactions.offer(new TxWithArrivalTimestamp(transaction), 10, TimeUnit.SECONDS);
    }

    public boolean addToBroadcasted(Transaction transaction) {
        return broadcastedTransactions.add(transaction);
    }

    public Collection<Transaction> getAllBroadcastedTransactions() {
        return new ArrayList<>(broadcastedTransactions);
    }

    public void addTxToBroadcastWhenConfirmed(Transaction tx, Transaction unconfirmedTransaction) {
        txToBroadcastWhenConfirmed.put(tx, unconfirmedTransaction);
    }

    public void putInCache(UnconfirmedTransaction unconfirmedTransaction) {
        if (transactionCache.size() < maxCachedTransactions) {
            transactionCache.put(unconfirmedTransaction.getId(), unconfirmedTransaction);
        }
    }

    public void initializeCache(Stream<UnconfirmedTransaction> unconfirmedTransactionStream) {
        if (cacheInitialized) {
            unconfirmedTransactionStream.forEach(e -> {});
            return;
        }
        synchronized (this) {
            if (cacheInitialized) {
                unconfirmedTransactionStream.forEach(e -> {});
                return;
            }
            unconfirmedTransactionStream.forEach(e -> transactionCache.put(e.getId(), e));
        }
        cacheInitialized = true;
    }

    public Set<UnconfirmedTransaction> getFromCacheSorted(List<String> exclude) {
        TreeSet<UnconfirmedTransaction> sortedUnconfirmedTransactions = new TreeSet<>(cachedUnconfirmedTransactionComparator);
        transactionCache.values().forEach(transaction -> {
            if (Collections.binarySearch(exclude, transaction.getStringId()) < 0) {
                sortedUnconfirmedTransactions.add(transaction);
            }
        });
        return sortedUnconfirmedTransactions;
    }

    public void clear() {
        transactionCache.clear();
        txToBroadcastWhenConfirmed.clear();
        broadcastedTransactions.clear();
        broadcastPendingTransactions.clear();
        processLaterQueue.clear();
    }

    public int txCacheSize() {
        return transactionCache.size();
    }

    public UnconfirmedTransaction getFromCacheSorted(long id) {
        return transactionCache.get(id);
    }

    public void broadcastLater(Transaction tx) {
        broadcastedTransactions.add(tx);
    }

    public void removeFromCache(long id) {
        transactionCache.remove(id);
    }

    public boolean isBroadcasted(Transaction transaction) {
        return broadcastedTransactions.contains(transaction);
    }

    public void removeBroadcasted(List<Transaction> transactions) {
        broadcastedTransactions.removeAll(transactions);
    }

    public Map<Transaction, Transaction> getAllBroadcastWhenConfirmedTransactions() {
        return new HashMap<>(txToBroadcastWhenConfirmed);
    }

    public void removeBroadcastedWhenConfirmedTransactions(Collection<Transaction> transactions) {
        transactions.forEach(txToBroadcastWhenConfirmed::remove);
    }

    public Transaction nextBroadcastPendingTransaction() throws InterruptedException {
        return broadcastPendingTransactions.take().getTx();
    }

    public List<Transaction> allPendingTransactions() {
        return broadcastPendingTransactions.stream().map(TxWithArrivalTimestamp::getTx).collect(Collectors.toList());
    }

    public int pendingBroadcastQueueSize() {
        return broadcastPendingTransactions.size();
    }


    public double pendingBroadcastQueueLoadFactor() {
        return 1.0 * broadcastPendingTransactions.size() / maxPendingBroadcastQueueSize;
    }

    public int processLaterQueueSize() {
        return processLaterQueue.size();
    }

    @Data
    private static class TxWithArrivalTimestamp {
        private final long arrivalTime = System.currentTimeMillis();
        private final Transaction tx;
    }
}
