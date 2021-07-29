/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ChatInfo;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorer;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@Singleton
public class TransactionServiceImpl implements TransactionService {

    private final DatabaseManager databaseManager;
    private final TimeService timeService;
    private final PropertiesHolder propertiesHolder;
    private final BlockchainConfig blockchainConfig;
    private final TransactionDao transactionDao;
    private final ShardDbExplorer shardDbExplorer;
    private final TransactionEntityToModelConverter toModelConverter;
    private final TransactionModelToEntityConverter toEntityConverter;

    @Inject
    public TransactionServiceImpl(DatabaseManager databaseManager, TimeService timeService, PropertiesHolder propertiesHolder, BlockchainConfig blockchainConfig, TransactionDao transactionDao, ShardDbExplorer shardDbExplorer, TransactionEntityToModelConverter toModelConverter, TransactionModelToEntityConverter toEntityConverter) {
        this.databaseManager = databaseManager;
        this.timeService = timeService;
        this.propertiesHolder = propertiesHolder;
        this.blockchainConfig = blockchainConfig;
        this.transactionDao = transactionDao;
        this.shardDbExplorer = shardDbExplorer;
        this.toModelConverter = toModelConverter;
        this.toEntityConverter = toEntityConverter;
    }

    @Override
    public Transaction findTransaction(long transactionId) {
        TransactionEntity entity = transactionDao.findTransaction(transactionId, databaseManager.getDataSource());
        return toModelConverter.convert(entity);
    }

    @Override
    public Transaction findTransactionCrossSharding(long transactionId, int height) {
        TransactionalDataSource dataSource = shardDbExplorer.getDatasourceWithShardingByTransactionId(transactionId);
        TransactionEntity entity = transactionDao.findTransaction(transactionId, height, dataSource);
        return toModelConverter.convert(entity);
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash) {
        TransactionEntity entity = transactionDao.findTransactionByFullHash(fullHash, databaseManager.getDataSource());
        return toModelConverter.convert(entity);
    }

    @Override
    public Transaction findTransactionCrossShardingByFullHash(byte[] fullHash, int height) {
        TransactionalDataSource dataSource = shardDbExplorer.getDatasourceWithShardingByTransactionId(Convert.transactionFullHashToId(fullHash));
        TransactionEntity entity = transactionDao.findTransactionByFullHash(fullHash, height, dataSource);
        return toModelConverter.convert(entity);
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        return transactionDao.hasTransaction(transactionId, databaseManager.getDataSource());
    }

    @Override
    public boolean hasTransaction(long transactionId, int height) {
        return transactionDao.hasTransaction(transactionId, height, databaseManager.getDataSource());
    }

    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash) {
        return transactionDao.hasTransactionByFullHash(fullHash, databaseManager.getDataSource());
    }

    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash, int height) {
        return transactionDao.hasTransactionByFullHash(fullHash, height, databaseManager.getDataSource());
    }

    @Override
    public byte[] getFullHash(long transactionId) {
        return transactionDao.getFullHash(transactionId, databaseManager.getDataSource());
    }

    @Override
    public List<Transaction> findBlockTransactionsCrossSharding(long blockId) {
        TransactionalDataSource dataSource = shardDbExplorer.getDataSourceWithSharding(blockId);
        List<TransactionEntity> transactions = transactionDao.findBlockTransactions(blockId, dataSource);
        return transactions.stream().map(toModelConverter).collect(Collectors.toList());
    }

    @Override
    public List<PrunableTransaction> findPrunableTransactions(int minTimestamp, int maxTimestamp) {
        return transactionDao.findPrunableTransactions(minTimestamp, maxTimestamp);
    }

    @Override
    public long getBlockTransactionsCountCrossSharding(long blockId) {
        TransactionalDataSource dataSource = shardDbExplorer.getDataSourceWithSharding(blockId);
        return transactionDao.getBlockTransactionsCount(blockId, dataSource);
    }

    @Override
    public void saveTransactions(List<Transaction> transactions) {
        if (!transactions.isEmpty()) {
            transactionDao.saveTransactions(transactions.stream().map(toEntityConverter).collect(Collectors.toList()));
        }
    }

    @Override
    public void updateTransaction(Transaction transaction) {
        transactionDao.updateTransaction(toEntityConverter.convert(transaction));
    }

    @Override
    public int getTransactionCount() {
        return transactionDao.getTransactionCount();
    }

    @Override
    public Long getTransactionCount(int from, int to) {
        return transactionDao.getTransactionCount(databaseManager.getDataSource(), from, to);
    }

    @Override
    public List<Transaction> getTransactionsByFilter(long accountId, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly, int from, int to, boolean executedOnly, boolean includePrivate, int height, int prunableExpiration,  boolean failedOnly, boolean nonFailedOnly) {
        List<TransactionEntity> transactions = transactionDao.getTransactions(databaseManager.getDataSource(), accountId, type, subtype, blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, from, to, executedOnly, includePrivate, height, prunableExpiration, failedOnly, nonFailedOnly);
        return transactions.stream().map(toModelConverter).collect(Collectors.toList());
    }

    @Override
    public int getTransactionCountByFilter(long accountId, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly, boolean executedOnly, boolean includePrivate, int height, int prunableExpiration,  boolean failedOnly, boolean nonFailedOnly) {
        return transactionDao.getTransactionCountByFilter(databaseManager.getDataSource(), accountId, type, subtype, blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, executedOnly, includePrivate, height, prunableExpiration, failedOnly, nonFailedOnly);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Transaction> getTransactionsChatHistory(long account1, long account2, int from, int to) {
        List<TransactionEntity> transactions = transactionDao.getTransactionsChatHistory(account1, account2, from, to);
        return transactions.stream().map(toModelConverter).collect(Collectors.toList());
    }

    @Override
    public List<ChatInfo> getChatAccounts(long accountId, int from, int to) {
        return transactionDao.getChatAccounts(accountId, from, to);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Transaction> getTransactions(byte type, byte subtype, int from, int to) {
        List<TransactionEntity> transactions = transactionDao.getTransactions(type, subtype, from, to);
        return transactions.stream().map(toModelConverter).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<Transaction> getTransactions(int fromDbId, int toDbId) {
        List<TransactionEntity> transactions = transactionDao.getTransactions(fromDbId, toDbId);
        return transactions.stream().map(toModelConverter).collect(Collectors.toList());
    }

    @Override
    public List<TransactionDbInfo> getTransactionsBeforeHeight(int height) {
        return transactionDao.getTransactionsBeforeHeight(height);
    }

    @Override
    public int getTransactionCount(long accountId, byte type, byte subtype) {
        return transactionDao.getTransactionCount(accountId, type, subtype);
    }

    @Override
    public int getTransactionsCount(List<Long> accounts, byte type, byte subtype, int startTime, int endTime, int fromHeight, int toHeight, String sortOrder, int from, int to) {
        return transactionDao.getTransactionsCount(accounts, type, subtype, startTime, endTime, fromHeight, toHeight, sortOrder, from, to);
    }

    @Override
    public List<TxReceipt> getTransactions(List<Long> accounts, byte type, byte subtype, int startTime, int endTime, int fromHeight, int toHeight, String sortOrder, int from, int to) {
        return transactionDao.getTransactions(accounts, type, subtype, startTime, endTime, fromHeight, toHeight, sortOrder, from, to);
    }

    @Override
    public List<Transaction> getTransactionsCrossShardingByAccount(long accountId, int currentBlockChainHeight, int numberOfConfirmations, byte type, byte subtype,
                                                                   int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                                   int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate, boolean failedOnly, boolean nonFailedOnly) {
        long start = System.currentTimeMillis();
        int height = numberOfConfirmations > 0 ? currentBlockChainHeight - numberOfConfirmations : Integer.MAX_VALUE;
        int prunableExpiration = Math.max(0, propertiesHolder.INCLUDE_EXPIRED_PRUNABLE() && includeExpiredPrunable ?
            timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime() :
            timeService.getEpochTime() - blockchainConfig.getMinPrunableLifetime());
        int limit = to == Integer.MAX_VALUE ? Integer.MAX_VALUE : to - from + 1;
        log.trace("getTx() 1. from={}, to={}, initialLimit={}, accountId={}, type={}, subtype={}",
            from, to, limit, accountId, type, subtype);
        if (limit > 500) { // warn for too big values
            log.warn("Computed limit is BIGGER then 500 = {} !!", limit);
        }

        // start fetch from main db
        TransactionalDataSource currentDataSource = databaseManager.getDataSource();
        List<TransactionEntity> transactions = transactionDao.getTransactions(
            currentDataSource,
            accountId, type, subtype,
            blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
            from, to, executedOnly, includePrivate, height, prunableExpiration, failedOnly, nonFailedOnly);
        int foundCount = transactionDao.getTransactionCountByFilter(currentDataSource,
            accountId, type, subtype,
            blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, executedOnly, includePrivate, height, prunableExpiration, failedOnly, nonFailedOnly);
        log.trace("getTx() 2. fetched from mainDb, fetch=[{}] / foundCount={}, initLimit={}, accountId={}, type={}, subtype={}",
            transactions.size(), foundCount, limit, accountId, type, subtype);

        // check if all Txs are fetched from main db, continue inside shard dbs otherwise
        if (transactions.size() < limit) {
            // not all requested Tx were fetched from main, loop over shards
            if (transactions.size() > 0) {
                to -= (transactions.size() + from); // decrease limit by really fetched records
                from = 0; // set to zero for shard db
            } else {
                from -= foundCount;
                to -= foundCount;
            }
            // loop over all shard in descent order and fetch left Tx number
            Iterator<TransactionalDataSource> fullDataSources = ((ShardManagement) databaseManager).getAllFullDataSourcesIterator();
            while (fullDataSources.hasNext()) {
                TransactionalDataSource dataSource = fullDataSources.next();
                // count Tx records before fetch Tx records
                foundCount = transactionDao.getTransactionCountByFilter(dataSource,
                    accountId, type, subtype,
                    blockTimestamp, withMessage, phasedOnly, nonPhasedOnly, executedOnly, includePrivate, height, prunableExpiration, failedOnly, nonFailedOnly);
                log.trace("countTx 3. DS={}, from={} to={}, foundCount={} (skip='{}')\naccountId={}, type={}, subtype={}",
                    dataSource.getDbIdentity(), from, to, foundCount, (foundCount <= 0),
                    accountId, type, subtype);
                if (foundCount == 0) {
                    continue; // skip shard without any suitable records
                }
                // because count is > 0 then try to fetch Tx records from shard db
                List<TransactionEntity> fetchedTxs = transactionDao.getTransactions(
                    dataSource,
                    accountId, type, subtype,
                    blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
                    from, to, executedOnly, includePrivate, height, prunableExpiration, failedOnly, nonFailedOnly);
                log.trace("getTx() 4. DS={} fetched [{}] (foundCount={}) from={}, to={}", dataSource.getDbIdentity(),
                    fetchedTxs.size(), foundCount, from, to);
                if (fetchedTxs.isEmpty()) {
                    to -= foundCount;
                    from -= foundCount;
                } else {
                    to -= (fetchedTxs.size() + from);
                    from = 0;
                }
                transactions.addAll(fetchedTxs);
                if (transactions.size() >= limit) { // by default, size of fetched transactions should be equal to initialLimit, but when error occurred this check allow to avoid fetching all txs
                    break;
                }
            }
        }
        log.trace("Tx number Requested / Loaded : [{}] / [{}] = in {} ms", limit, transactions.size(), System.currentTimeMillis() - start);
        return transactions.stream().map(toModelConverter).collect(Collectors.toList());
    }

}
