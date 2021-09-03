/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.cache.RemovedTxsCacheConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.UnconfirmedTransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.google.common.cache.Cache;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class MemPool {
    private final IteratorToStreamConverter<UnconfirmedTransactionEntity> streamConverter = new IteratorToStreamConverter<>();
    private final UnconfirmedTransactionTable table;
    private final MemPoolInMemoryState memoryState;
    private final UnconfirmedTransactionEntityToModelConverter toModelConverter;
    private final UnconfirmedTransactionModelToEntityConverter toEntityConverter;
    private final Cache<Long, RemovedTx> removedTransactions;
    @Getter
    private final MemPoolConfig config;

    @Inject
    public MemPool(UnconfirmedTransactionTable table,
                   UnconfirmedTransactionEntityToModelConverter toModelConverter,
                   UnconfirmedTransactionModelToEntityConverter toEntityConverter,
                   InMemoryCacheManager inMemoryCacheManager,
                   MemPoolConfig config
                  ) {
        this(table, new MemPoolInMemoryState(config), toModelConverter, toEntityConverter, inMemoryCacheManager.acquireCache(RemovedTxsCacheConfig.CACHE_NAME), config);
    }

    public MemPool(UnconfirmedTransactionTable table, MemPoolInMemoryState memoryState, UnconfirmedTransactionEntityToModelConverter toModelConverter, UnconfirmedTransactionModelToEntityConverter toEntityConverter, Cache<Long, RemovedTx> removedTransactions, MemPoolConfig config) {
        this.table = table;
        this.memoryState = memoryState;
        this.toModelConverter = toModelConverter;
        this.toEntityConverter = toEntityConverter;
        this.removedTransactions = removedTransactions;
        this.config = config;
    }

    public void initCache() {
        // Initialize the unconfirmed transaction cache if it hasn't been done yet
        if (!memoryState.isCacheInitialized()) {
            memoryState.initializeCache(streamConverter.apply(table.getAll(0, -1)).map(toModelConverter));
        }
    }

    public Transaction get(long id) {
        Transaction transaction = memoryState.getFromCache(id);
        if (transaction != null) {
            return transaction;
        }
        return toModelConverter.convert(table.getById(id));
    }

    public void markRemoved(long id) {
        removedTransactions.put(id, new RemovedTx(id, System.currentTimeMillis()));
        memoryState.removePendingProcessing(id);
    }

    public boolean hasSaved(long id) {
        return  get(id) != null;
    }

    public boolean contains(long id) {
        return memoryState.pendingProcessingContains(id) || get(id) != null;
    }

    public Collection<Transaction> getAllBroadcasted() {
        return memoryState.getAllBroadcasted();
    }

    public void broadcastWhenConfirmed(Transaction tx, Transaction unconfirmedTx) {
        memoryState.addTxToBroadcastWhenConfirmed(tx, unconfirmedTx);
    }

    public Set<UnconfirmedTransaction> getCached(List<String> exclude) {
        return memoryState.getFromCache(exclude);
    }

    public boolean addProcessed(UnconfirmedTransaction tx) {
        boolean canSaveTxs = getCount() < config.getMaxUnconfirmedTransactions();
        if (canSaveTxs) {
            table.insert(toEntityConverter.convert(tx));
            memoryState.putInCache(tx);
            log.info("Added transaction {} into a mempool", tx.getStringId());
        }
        return canSaveTxs;
    }

    /**
     * Add unconfirmed transaction to the pending-processing queue
     * @param tx unconfirmed transaction to add
     * @throws AplMemPoolFullException when pending-processing queue is full
     * @throws AplTransactionIsAlreadyInMemPoolException when pending-processing queue already contains give transaction
     */
    public void addPendingProcessing(UnconfirmedTransaction tx) {
        IdQueue.ReturnCode returnCode = memoryState.addPendingProcessing(tx);
        if (!returnCode.isOk()) {
            if (returnCode == IdQueue.ReturnCode.NOT_ADDED || returnCode == IdQueue.ReturnCode.FULL) {
                throw new AplMemPoolFullException("Pending transaction's queue size is reached, unable to add new transaction: " + tx.getId());
            }
            if (returnCode == IdQueue.ReturnCode.ALREADY_EXIST) {
                throw new AplTransactionIsAlreadyInMemPoolException("Transaction " + tx.getId() + " is already in the pending transaction's queue");
            }
        }
    }

    public int processingQueueSize() {
        return memoryState.pendingProcessingSize();
    }

    public UnconfirmedTransaction nextPendingProcessing() {
        return memoryState.nextPendingProcessing();
    }


    public boolean canAcceptReferenced() {
        return memoryState.referencedRemainingCapacity() > 0;
    }

    public Stream<UnconfirmedTransaction> getAllStream() {
        return table.getAllUnconfirmedTransactions().map(toModelConverter);
    }

    public int getCount() {
        if(getCachedCount() < config.getMaxCachedTransactions()) {
            return getCachedCount();
        } else {
            return table.getCount();
        }
    }

    public int getSavedCount() {
        return table.getCount();
    }

    public int getCachedCount() {
        return memoryState.txCacheSize();
    }

    public void removeBroadcasted(Transaction transaction) {
        memoryState.removeBroadcasted(List.of(transaction));
    }

    public boolean canAccept(int numTx) {
        return remainingCapacity() - numTx >= 0;
    }

    public int remainingCapacity() {
        return config.getMaxUnconfirmedTransactions() - getCount();
    }

    public int pendingProcessingRemainingCapacity() {
        return memoryState.pendingProcessingReminingCapacity();
    }

    public List<UnconfirmedTransaction> getAllPendingProcessing() {
        return memoryState.getAllPendingProcessing();
    }

    public Stream<UnconfirmedTransaction> getAllStream(int from, int to) {
        return streamConverter.apply(table.getAll(from, to)).map(toModelConverter);
    }

    public void processLater(UnconfirmedTransaction unconfirmedTransaction) {
        removedTransactions.invalidate(unconfirmedTransaction.getId());
        memoryState.processLater(unconfirmedTransaction);
    }

    public Iterator<UnconfirmedTransaction> processLaterIterator() {
        return memoryState.processLaterIterator();
    }

    public int getProcessLaterCount() {
        return memoryState.processLaterSize();
    }

    public void clear() {
        memoryState.clear();
        table.truncate();
    }

    public void rebroadcastAll() {
        CollectionUtil.forEach(getAllStream(), e -> {
            if (config.isEnableRebroadcasting()) {
                memoryState.addToBroadcasted(e.getTransactionImpl());
            }
        });
    }

    public boolean remove(Transaction transaction) {
        int deleted = table.deleteById(transaction.getId());
        memoryState.removeCached(transaction);
        removedTransactions.put(transaction.getId(), new RemovedTx(transaction.getId(), System.currentTimeMillis()));
        return deleted > 0;
    }

    public boolean isRemoved(Transaction transaction) {
        return removedTransactions.getIfPresent(transaction.getId()) != null;
    }

    public List<Long> getAllRemoved(int limit) {
        ArrayList<RemovedTx> listOfRemovedTxs = new ArrayList<>(removedTransactions.asMap().values());
        return listOfRemovedTxs.stream().sorted(Comparator.comparingLong(RemovedTx::getTime).reversed()).map(RemovedTx::getId).limit(limit).collect(Collectors.toList());
    }

    public int getRemovedSize() {
        return (int) removedTransactions.size();
    }

    public int getReferencedCount() {
        return memoryState.getReferencedCount();
    }

    public void rebroadcast(Transaction tx) {
        if (config.isEnableRebroadcasting()) {
            memoryState.addToBroadcasted(tx);
        }
    }

    public List<Long> getAllIds() {
        if(getCachedCount() < config.getMaxCachedTransactions()){
            return memoryState.getAllCachedIds();
        } else {
            return table.getAllUnconfirmedTransactionIds();
        }
    }

    public int getExpiredCount(int epochTime) {
        return table.countExpiredTransactions(epochTime);
    }


    public Stream<UnconfirmedTransaction> getExpiredStream(int epochTime) {
        return table.getExpiredTxsStream(epochTime).map(toModelConverter);
    }

    public boolean isAlreadyBroadcasted(Transaction transaction) {
        return memoryState.isBroadcasted(transaction);
    }

    public void removeBroadcasted(List<Transaction> transactions) {
        memoryState.removeBroadcasted(transactions);
    }

    public Map<Transaction, Transaction> getAllBroadcastWhenConfirmed() {
        return memoryState.getAllBroadcastWhenConfirmed();
    }

    public void removeBroadcastedWhenConfirmed(Collection<Transaction> transactions) {
        memoryState.removeBroadcastedWhenConfirmed(transactions);
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class RemovedTx {
        @EqualsAndHashCode.Include
        private final long id;
        private final long time;
    }
}
