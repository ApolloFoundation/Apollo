/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.WrappedTransaction;
import com.apollocurrency.aplwallet.apl.core.db.DbTransactionHelper;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTxValidationResult;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
// TODO cache rollback, when db transaction fails
public class ProcessUnconfirmedTransactionsQueueTask implements Runnable {
    private final MemPool memPool;
    private final BatchSizeCalculator batchSizeCalculator;
    private final TransactionValidator validator;
    private final UnconfirmedTransactionProcessingService processingService;
    private final DatabaseManager databaseManager;

    public ProcessUnconfirmedTransactionsQueueTask(MemPool memPool,
                                                   TransactionValidator validator,
                                                   UnconfirmedTransactionProcessingService processingService,
                                                   BatchSizeCalculator batchSizeCalculator,
                                                   DatabaseManager databaseManager) {
        this.memPool = Objects.requireNonNull(memPool);
        this.validator = Objects.requireNonNull(validator);
        this.processingService = Objects.requireNonNull(processingService);
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.batchSizeCalculator = Objects.requireNonNull(batchSizeCalculator);
        log.info("Created 'ProcessUnconfirmedTransactionsQueueTask' instance");
    }

    @Override
    public void run() {
        try {
            processBatch();
        } catch (Throwable e) {
            log.error("Error during processing unconfirmed transaction from queue ", e);
        }
    }

    void processBatch() {
        List<UnconfirmedTransaction> addedTransactions =  DbTransactionHelper.executeInTransaction(databaseManager.getDataSource(), ()-> {
            int number = batchSizeCalculator.currentBatchSize();
            List<UnconfirmedTransaction> transactions = collectBatch(number);
            if (!transactions.isEmpty()) {
                return batchSizeCalculator.doTimedOp(batchSize-> {
                    log.debug("Processing batch size {}, transactions {}", number, transactions.size());
                    return addToMempool(transactions);
                });
            }
            return Collections.emptyList();
        });
        if (!addedTransactions.isEmpty()) {
            log.info("Added to mempool [{}]", addedTransactions.stream().map(WrappedTransaction::getId).map(String::valueOf).collect(Collectors.joining(",")));
        }
    }

    private List<UnconfirmedTransaction> addToMempool(List<UnconfirmedTransaction> transactions) {
        List<UnconfirmedTransaction> addedTxs = new ArrayList<>();
        for (UnconfirmedTransaction transaction : transactions) {
            log.trace("Processing transaction {}", transaction.getId());
            UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(transaction);
            if (validationResult.isOk()) {
                try {
                    validator.validateSignatureWithTxFeeLessStrict(transaction);
                    validator.validateLightly(transaction);
                    boolean added = memPool.addProcessed(transaction);
                    if (!added) {
                        log.warn("Unable to add new unconfirmed transaction {}, mempool is full", transaction.getId());
                    } else {
                        addedTxs.add(transaction);
                    }
                } catch (AplException.ValidationException e) {
                    log.trace("Invalid transaction {}, reason {}", transaction.getId(), e.getMessage());
                }
            }
        }
        return addedTxs;
    }

    private List<UnconfirmedTransaction> collectBatch(int number) {
        List<UnconfirmedTransaction> collectedTxs = new ArrayList<>();
        int allowedBatch = calculateAllowedBatch(number);
        if (allowedBatch == 0) { // do not loose existing transactions
            return collectedTxs;
        }
        int collected = 0;
        while (collected < allowedBatch) {
            Optional<UnconfirmedTransaction> nextTxOptional = nextTransaction();
            if (nextTxOptional.isPresent()) {
                collected++;
                collectedTxs.add(nextTxOptional.get());
            } else {
                break;
            }
        }
        return collectedTxs;
    }

    private int calculateAllowedBatch(int desirableBatch) {
        int pendingTxCount = memPool.processingQueueSize();
        if(pendingTxCount > 0) {
            // memPool.canSafelyAccept() make call to the db.
            return Math.min(memPool.remainingCapacity(), Math.min(desirableBatch, pendingTxCount));
        } else {
            return pendingTxCount;
        }
    }

    Optional<UnconfirmedTransaction> nextTransaction() {
        if (memPool.processingQueueSize() > 0) {
            UnconfirmedTransaction tx = memPool.nextPendingProcessing();
            return Optional.of(tx);
        } else {
            return Optional.empty();
        }
    }
}
