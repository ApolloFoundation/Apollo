/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.model.AplQueryObject;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TxReceiptMapper;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Find transaction service. It doesn't support the sharding DB, uses only the main DB.*
 *
 * @author andrii.zinchenko@firstbridge.io
 */

@Slf4j
@Singleton
public class FindTransactionServiceImpl implements FindTransactionService {
    private final BlockChainInfoService blockChainInfoService;
    private final TransactionService transactionService;
    private final MemPool memPool;
    private final TxReceiptMapper txReceiptMapper;

    @Inject
    public FindTransactionServiceImpl(TransactionService transactionService,
                                      MemPool memPool,
                                      BlockChainInfoService blockChainInfoService,
                                      TxReceiptMapper txReceiptMapper) {
        this.transactionService = Objects.requireNonNull(transactionService);
        this.blockChainInfoService = Objects.requireNonNull(blockChainInfoService);
        this.txReceiptMapper = Objects.requireNonNull(txReceiptMapper);
        this.memPool = memPool;
    }

    @Override
    public Stream<UnconfirmedTransaction> getAllUnconfirmedTransactionsStream() {
        return memPool.getAllProcessedStream();
    }

    @Override
    public Optional<Transaction> findTransaction(long transactionId, int height) {
        return Optional.ofNullable(transactionService.findTransactionCrossSharding(transactionId, height));
    }

    @Override
    public Optional<Transaction> findUnconfirmedTransaction(long transactionId) {
        return Optional.ofNullable(memPool.getUnconfirmedTransaction(transactionId));
    }

    @Override
    public List<TxReceipt> getConfirmedTransactionsByQuery(AplQueryObject query) {
        return getTransactionsByQuery(query, false);
    }

    @Override
    public List<TxReceipt> getTransactionsByQuery(AplQueryObject query, boolean includeUnconfirmed) {
        if (query.getStartTime() <= 0 || query.getEndTime() <= 0) {
            throw new IllegalArgumentException("Wrong time stamp values: timeStart=" + query.getStartTime() + " timeEnd=" + query.getEndTime());
        }
        Stream<Transaction> unconfirmedTransactionStream = null;
        if (includeUnconfirmed && query.getLastHeight() <= 0) {
            unconfirmedTransactionStream = getAllUnconfirmedTransactionsStream()
                .filter(transaction -> transaction.getTimestamp() > query.getStartTime() && transaction.getTimestamp() < query.getEndTime())
                .map(unconfirmedTransaction -> unconfirmedTransaction);
        }

        int height = blockChainInfoService.getHeight();

        Stream<TxReceipt> transactionStream = transactionService.getTransactions(query.getAccounts(), query.getType(), (byte) -1,
            query.getStartTime(), query.getEndTime(),
            query.getFirstHeight(), query.getLastHeight(),
            query.getOrder().name(),
            query.getFrom(), query.getTo())
            .stream().peek(txReceipt -> {
                    txReceipt.setConfirmations(Math.max(0, height - txReceipt.getHeight()));
                    txReceipt.setStatus(TxReceipt.StatusEnum.CONFIRMED);
                }
            );

        return unconfirmedTransactionStream != null ?
            Stream.concat(unconfirmedTransactionStream.map(txReceiptMapper), transactionStream)
                .collect(Collectors.toList())
            : transactionStream.collect(Collectors.toUnmodifiableList());
    }

    @Override
    public long getConfirmedTransactionsCountByQuery(AplQueryObject query) {
        return getTransactionsCountByQuery(query, false);
    }

    @Override
    public long getTransactionsCountByQuery(AplQueryObject query, boolean includeUnconfirmed) {
        if (query.getStartTime() <= 0 || query.getEndTime() <= 0) {
            throw new IllegalArgumentException("Wrong time stamp values: timeStart=" + query.getStartTime() + " timeEnd=" + query.getEndTime());
        }
        long unconfirmedTxCount = 0;
        if (includeUnconfirmed && query.getLastHeight() <= 0) {
            unconfirmedTxCount = getAllUnconfirmedTransactionsStream()
                .filter(transaction -> transaction.getTimestamp() > query.getStartTime() && transaction.getTimestamp() < query.getEndTime())
                .count();
        }

        long txCount = transactionService.getTransactionsCount(query.getAccounts(), query.getType(), (byte) -1,
            query.getStartTime(), query.getEndTime(),
            query.getFirstHeight(), query.getLastHeight(),
            query.getOrder().name(),
            query.getFrom(), query.getTo());

        return unconfirmedTxCount + txCount;
    }



}
