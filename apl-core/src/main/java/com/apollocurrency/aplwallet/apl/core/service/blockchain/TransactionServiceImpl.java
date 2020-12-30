/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;

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
    private final TransactionEntityToModelConverter toModelConverter;

    @Inject
    public TransactionServiceImpl(DatabaseManager databaseManager, TimeService timeService, PropertiesHolder propertiesHolder, BlockchainConfig blockchainConfig, TransactionDao transactionDao, TransactionEntityToModelConverter toModelConverter) {
        this.databaseManager = databaseManager;
        this.timeService = timeService;
        this.propertiesHolder = propertiesHolder;
        this.blockchainConfig = blockchainConfig;
        this.transactionDao = transactionDao;
        this.toModelConverter = toModelConverter;
    }

    @Override
    public Transaction findTransaction(long transactionId) {
        TransactionEntity entity = transactionDao.findTransaction(transactionId, databaseManager.getDataSource());
        return toModelConverter.convert(entity);
    }

    @Override
    public Transaction findTransaction(long transactionId, int height) {
        TransactionEntity entity = transactionDao.findTransaction(transactionId, height, databaseManager.getDataSource());
        return toModelConverter.convert(entity);
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash) {
        return null;
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash, int height) {
        return null;
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        return false;
    }

    @Override
    public boolean hasTransaction(long transactionId, int height) {
        return false;
    }

    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash) {
        return false;
    }

    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash, int height) {
        return false;
    }

    @Override
    public byte[] getFullHash(long transactionId) {
        return new byte[0];
    }

    @Override
    public List<Transaction> findBlockTransactions(long blockId) {
        return null;
    }

    @Override
    public long getBlockTransactionsCount(long blockId) {
        return 0;
    }

    @Override
    public void saveTransactions(List<Transaction> transactions) {

    }

    @Override
    public void saveTransactions(Connection con, List<Transaction> transactions) {
        transactionDao.saveTransactions(con, transactions);
    }

    @Override
    public int getTransactionCount() {
        return 0;
    }

    @Override
    public Long getTransactionCount(int from, int to) {
        return null;
    }

    @Override
    public List<Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly, int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate, int height, int prunableExpiration) {
        return null;
    }

    @Override
    public int getTransactionCountByFilter(long accountId, int numberOfConfirmations, byte type, byte subtype, int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate, int height, int prunableExpiration) {
        return 0;
    }

    @Override
    public List<Transaction> getTransactions(byte type, byte subtype, int from, int to) {
        return null;
    }

    @Override
    public List<Transaction> getTransactions(int fromDbId, int toDbId) {
        return null;
    }

    @Override
    public List<TransactionDbInfo> getTransactionsBeforeHeight(int height) {
        return null;
    }

    @Override
    public int getTransactionCount(long accountId, byte type, byte subtype) {
        return 0;
    }

    @Override
    public int getTransactionsCount(List<Long> accounts, byte type, byte subtype, int startTime, int endTime, int fromHeight, int toHeight, String sortOrder, int from, int to) {
        return 0;
    }

    @Override
    public List<TxReceipt> getTransactions(List<Long> accounts, byte type, byte subtype, int startTime, int endTime, int fromHeight, int toHeight, String sortOrder, int from, int to) {
        return null;
    }

    @Override
    public List<Transaction> getTransactions(long accountId, int currentBlockChainHeight, int numberOfConfirmations, byte type, byte subtype,
                                             int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                             int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate) {
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
        List<Transaction> transactions = transactionDao.getTransactions(
            currentDataSource,
            accountId, numberOfConfirmations, type, subtype,
            blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
            from, to, includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
        int foundCount = transactionDao.getTransactionCountByFilter(currentDataSource,
            accountId, numberOfConfirmations, type, subtype,
            blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
            includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
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
                    accountId, numberOfConfirmations, type, subtype,
                    blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
                    includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
                log.trace("countTx 3. DS={}, from={} to={}, foundCount={} (skip='{}')\naccountId={}, type={}, subtype={}",
                    dataSource.getDbIdentity(), from, to, foundCount, (foundCount <= 0),
                    accountId, type, subtype);
                if (foundCount == 0) {
                    continue; // skip shard without any suitable records
                }
                // because count is > 0 then try to fetch Tx records from shard db
                List<Transaction> fetchedTxs = transactionDao.getTransactions(
                    dataSource,
                    accountId, numberOfConfirmations, type, subtype,
                    blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
                    from, to, includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
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
        return transactions;
    }
}
