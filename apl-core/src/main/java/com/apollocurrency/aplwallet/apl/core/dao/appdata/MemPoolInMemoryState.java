/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionComparator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
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

    private final Map<Long, UnconfirmedTransaction> transactionCache = new HashMap<>();
    private final Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> unconfirmedDuplicates = new HashMap<>();
    private final Map<Transaction, Transaction> txToBroadcastWhenConfirmed = new ConcurrentHashMap<>();
    private final Set<Transaction> broadcastedTransactions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final UnconfirmedTransactionCreator unconfirmedTransactionCreator;
    private final PriorityQueue<UnconfirmedTransaction> waitingTransactions;
    private final PriorityBlockingQueue<TxWithArrivalTimestamp> broadcastPendingTransactions;

    @Getter
    private final int maxInMemorySize;
    @Getter
    private volatile boolean cacheInitialized;
    private final int maxPendingBroadcastQueueSize;


    @Inject
    public MemPoolInMemoryState(UnconfirmedTransactionCreator unconfirmedTransactionCreator, @Property(name = "apl.maxUnconfirmedTransactions", defaultValue = "" + Integer.MAX_VALUE) int maxUnconfirmedTransactions) {
        this.unconfirmedTransactionCreator = unconfirmedTransactionCreator;
        this.maxInMemorySize = maxUnconfirmedTransactions;
        this.waitingTransactions = new SizeBoundedPriorityQueue<>(maxUnconfirmedTransactions, new UnconfirmedTransactionComparator());
        this.maxPendingBroadcastQueueSize = 20_000;
        this.broadcastPendingTransactions = new PriorityBlockingQueue<>(maxPendingBroadcastQueueSize, Comparator.comparing(TxWithArrivalTimestamp::getArrivalTime));
    }

    public void backToWaiting(UnconfirmedTransaction unconfirmedTransaction) {
        waitingTransactions.add(unconfirmedTransaction);
        transactionCache.remove(unconfirmedTransaction.getId());
    }

    public boolean addToSoftBroadcastingQueue(Transaction transaction) {
        return broadcastPendingTransactions.offer(new TxWithArrivalTimestamp(transaction), 10, TimeUnit.SECONDS);
    }

    public boolean addToBroadcasted(Transaction transaction) {
        return broadcastedTransactions.add(transaction);
    }

    public boolean addToWaitingQueue(UnconfirmedTransaction transaction) {
        return waitingTransactions.add(transaction);
    }

    public List<UnconfirmedTransaction> waitingTransactions() {
        ArrayList<UnconfirmedTransaction> unconfirmedTransactions = new ArrayList<>(waitingTransactions);
        unconfirmedTransactions.sort(waitingTransactions.comparator());
        return unconfirmedTransactions;
    }

    public int waitingQueueSize() {
        return waitingTransactions.size();
    }

    public Iterator<UnconfirmedTransaction> waitingQueueIterator() {
        return waitingTransactions.iterator();
    }

    public boolean isWaitingQueueFull() {
        return waitingTransactions.size() == maxInMemorySize;
    }

    public Collection<Transaction> getAllBroadcastedTransactions() {
        return new ArrayList<>(broadcastedTransactions);
    }

    public void addTxToBroadcastWhenConfirmed(Transaction tx, Transaction unconfirmedTransaction) {
        txToBroadcastWhenConfirmed.put(tx, unconfirmedTransaction);
    }


    public void resetUnconfirmedDuplicates() {
        unconfirmedDuplicates.clear();
    }

    public void putInCache(UnconfirmedTransaction unconfirmedTransaction) {
        if (transactionCache.size() < maxInMemorySize) {
            transactionCache.put(unconfirmedTransaction.getId(), unconfirmedTransaction);
        }
    }

    public void initializeCache(Stream<UnconfirmedTransaction> unconfirmedTransactionStream) {
        if (cacheInitialized) {
            unconfirmedTransactionStream.forEach(e-> {});
            return;
        }
        synchronized (transactionCache) {
            if (cacheInitialized) {
                unconfirmedTransactionStream.forEach(e-> {});
                return;
            }
            unconfirmedTransactionStream.forEach(e-> transactionCache.put(e.getId(), e));
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
        unconfirmedDuplicates.clear();
        txToBroadcastWhenConfirmed.clear();
        broadcastedTransactions.clear();
        waitingTransactions.clear();
        broadcastPendingTransactions.clear();
    }

    public UnconfirmedTransaction getFromCacheSorted(long id) {
        return transactionCache.get(id);
    }

    public void broadcastLater(Transaction tx) {
        waitingTransactions.add(unconfirmedTransactionCreator.from(tx));
        broadcastedTransactions.add(tx);
    }

    public boolean isDuplicate(Transaction transaction) {
        return transaction.isUnconfirmedDuplicate(unconfirmedDuplicates);
    }

    public void resetProcessedState() {
        transactionCache.clear();
        unconfirmedDuplicates.clear();

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

    public int pendingBroadcastQueueSize() {
        return broadcastPendingTransactions.size();
    }


    public double pendingBroadcastQueueLoadFactor() {
        return 1.0 * broadcastPendingTransactions.size() / maxPendingBroadcastQueueSize;
    }

    @Data
    private static class TxWithArrivalTimestamp {
        private final long arrivalTime = System.currentTimeMillis();
        private final Transaction tx;
    }
}
