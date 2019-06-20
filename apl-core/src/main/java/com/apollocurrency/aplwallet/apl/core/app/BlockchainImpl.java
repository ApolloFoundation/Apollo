/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.core.phasing.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class BlockchainImpl implements Blockchain {
    /**
     * Specify offset from current height to retrieve block generators [currentHeight - offset; currentHeight] for further tracking generator hitTime
     */
    private static final int MAX_BLOCK_GENERATOR_OFFSET = 10_000;
    private BlockDao blockDao;
    private TransactionDao transactionDao;
    private BlockchainConfig blockchainConfig;
    private EpochTime timeService;
    private PropertiesHolder propertiesHolder;
    private TransactionIndexDao transactionIndexDao;
    private BlockIndexDao blockIndexDao;
    private DatabaseManager databaseManager;


    @Inject
    public BlockchainImpl(BlockDao blockDao, TransactionDao transactionDao, BlockchainConfig blockchainConfig, EpochTime timeService,
                          PropertiesHolder propertiesHolder, TransactionIndexDao transactionIndexDao, BlockIndexDao blockIndexDao, DatabaseManager databaseManager) {
        this.blockDao = blockDao;
        this.transactionDao = transactionDao;
        this.blockchainConfig = blockchainConfig;
        this.timeService = timeService;
        this.propertiesHolder = propertiesHolder;
        this.transactionIndexDao = transactionIndexDao;
        this.blockIndexDao = blockIndexDao;
        this.databaseManager = databaseManager;
    }

    private final AtomicReference<Block> lastBlock = new AtomicReference<>();



    @Override
    public Block getLastBlock() {
        return lastBlock.get();
    }

    public void setLastBlock(Block block) {
        lastBlock.set(block);
    }

    @Override
    public int getHeight() {
        Block last = lastBlock.get();
        return last == null ? 0 : last.getHeight();
    }

    @Override
    public int getLastBlockTimestamp() {
        Block last = lastBlock.get();
        return last == null ? 0 : last.getTimestamp();
    }

    @Override
    public Block getLastBlock(int timestamp) {
        Block block = lastBlock.get();
        if (timestamp >= block.getTimestamp()) {
            return block;
        }
        return blockDao.findLastBlock(timestamp);
    }

    @Override
    @Transactional(readOnly = true)
    public Block getBlock(long blockId) {
        Block block = lastBlock.get();
        if (block.getId() == blockId) {
            return block;
        }
        TransactionalDataSource dataSource = getDataSourceWithSharding(blockId);

        return blockDao.findBlock(blockId, dataSource);
    }

    @Override
    public boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE);
    }

/*
    @Override
    public DbIterator<Block> getAllBlocks() {
        return lookupBlockDao().getAllBlocks();
    }
*/

    @Override
    public DbIterator<Block> getBlocks(int from, int to) {
        int blockchainHeight = getHeight();
        int calculatedFrom = blockchainHeight - from;
        int calculatedTo = blockchainHeight - to;
        return blockDao.getBlocks(calculatedFrom, calculatedTo);
    }

/*
    @Override
    public DbIterator<Block> getBlocks(long accountId, int timestamp) {
        return getBlocks(accountId, timestamp, 0, -1);
    }
*/

    @Override
    public DbIterator<Block> getBlocks(long accountId, int timestamp, int from, int to) {
        return blockDao.getBlocks(accountId, timestamp, from, to);
    }

    @Override
    public Block findLastBlock() {
        return blockDao.findLastBlock();
    }

    @Override
    public Block loadBlock(Connection con, ResultSet rs, boolean loadTransactions) {
        Block block = blockDao.loadBlock(con, rs);
        if (loadTransactions) {
            List<Transaction> blockTransactions = transactionDao.findBlockTransactions(con, block.getId());
            block.setTransactions(blockTransactions);
        }
        return block;
    }

    @Override
    public void saveBlock(Connection con, Block block) {
        blockDao.saveBlock(con, block);
        transactionDao.saveTransactions(con, block.getTransactions());
    }

    @Override
    public void commit(Block block) {
        blockDao.commit(block);
    }

    @Override
    public Long getBlockCount(TransactionalDataSource dataSource, int from, int to) {
        return blockDao.getBlockCount(dataSource, from, to);
    }

    @Override
    public int getBlockCount(long accountId) {
        return blockDao.getBlockCount(accountId);
    }

/*
    @Override
    public DbIterator<Block> getBlocks(Connection con, PreparedStatement pstmt) {
        return lookupBlockDao().getBlocks(con, pstmt);
    }
*/

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        // Check the block cache

        List<Long> result = new ArrayList<>(limit);

//        synchronized (blockDao.getBlockCache()) {
//            Block block = blockDao.getBlockCache().get(blockId);
//            if (block != null) {
//                Collection<Block> cacheMap = blockDao.getHeightMap().tailMap(block.getHeight() + 1).values();
//                for (Block cacheBlock : cacheMap) {
//                    if (result.size() >= limit) {
//                        break;
//                    }
//                    result.add(cacheBlock.getId());
//                }
//                return result;
//            }
//        }
        Integer height = blockIndexDao.getHeight(blockId);
        if (height != null) {
            result.addAll(blockIndexDao.getBlockIdsAfter(height, limit));
        }
        if (result.size() < limit) {
            long lastBlockId = blockId;
            int idsRemaining = limit;
            if (result.size() > 0) {
                lastBlockId = result.get(result.size() - 1);
                idsRemaining -= result.size();
            }
            Integer lastBlockHeight = getBlockHeight(lastBlockId);
            if (lastBlockHeight != null) {
                List<Long> remainingIds = blockDao.getBlockIdsAfter(lastBlockHeight, idsRemaining);
                result.addAll(remainingIds);
            }
        }
        return result;
    }



    private Integer getBlockHeight(long blockId) {
        Integer height = blockIndexDao.getHeight(blockId);
        if (height == null) {
            Block block = blockDao.findBlock(blockId, databaseManager.getDataSource());
            if (block != null) {
                height = block.getHeight();
            }
        }
        return height;
    }

    @Override
    public List<byte[]> getBlockSignaturesFrom(int fromHeight, int toHeight) {
        return blockDao.getBlockSignaturesFrom(fromHeight, toHeight);
    }


    @Override
    public List<Block> getBlocksAfter(long blockId, List<Long> blockIdList) {
        // Check the block cache
        if (blockIdList.isEmpty()) {
            return Collections.emptyList();
        }
        log.debug("Block ids {}", blockIdList);
        log.debug("Has block {} - {}", blockId, hasBlockInShards(blockId));
        List<Block> result = new ArrayList<>();
        TransactionalDataSource dataSource;
        Integer fromBlockHeight = getBlockHeight(blockId);
        log.debug("Found block height {}", fromBlockHeight);
        if (fromBlockHeight != null) {
            int prevSize;
            do {
                dataSource = getDataSourceWithShardingByHeight(fromBlockHeight + 1); //should return datasource, where such block exist or default datasource
                prevSize = result.size();
                blockDao.getBlocksAfter(fromBlockHeight, blockIdList, result, dataSource, prevSize);
                log.debug("Got {} blocks from data_source {}", result.size(), dataSource.getUrl());
                for (int i = prevSize; i < result.size(); i++) {
                    Block block = result.get(i);
                    List<Transaction> blockTransactions = transactionDao.findBlockTransactions(block.getId(), dataSource);
                    block.setTransactions(blockTransactions);
                }
                if (result.size() - 1 >= 0) {
                    fromBlockHeight = getBlockHeight(blockIdList.get(result.size() - 1));
                }
            } while (result.size() != prevSize && dataSource != databaseManager.getDataSource() && getDataSourceWithShardingByHeight(fromBlockHeight + 1) != dataSource);
        }
        return result;
    }

    @Override
    public long getBlockIdAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block.getId();
        }
        BlockIndex blockIndex = blockIndexDao.getByBlockHeight(height);
        if (blockIndex != null) {
            return blockIndex.getBlockId();
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        return blockDao.findBlockIdAtHeight(height, dataSource);
    }

    @Override
    public Block getBlockAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block;
        }
        TransactionalDataSource dataSource = getDataSourceWithShardingByHeight(height);
        return blockDao.findBlockAtHeight(height, dataSource);
    }


    @Override
    @Transactional(readOnly = true)
    public Block getShardInitialBlock() {
        return getBlockAtHeight(getGenesisHeight());
    }

    private int getGenesisHeight() {
        Integer lastShardHeight = blockIndexDao.getLastHeight();
        return lastShardHeight != null ? lastShardHeight + 1 : 0;
    }

    @Override
    public EcBlockData getECBlock(int timestamp) {
        Block block = getLastBlock(timestamp);
        Block shardInitialBlock = getShardInitialBlock();
        if (block == null) {
            return new EcBlockData(shardInitialBlock.getId(), shardInitialBlock.getHeight());
        }
        int height = Math.max(block.getHeight() - 720, shardInitialBlock.getHeight());
        Block ecBlock = blockDao.findBlockAtHeight(height, databaseManager.getDataSource());
        return new EcBlockData(ecBlock.getId(), ecBlock.getHeight());
    }

    @Override
    public void deleteBlocksFromHeight(int height) {
        blockDao.deleteBlocksFromHeight(height);
    }

    @Override
    public Block deleteBlocksFrom(long blockId) {
        return blockDao.deleteBlocksFrom(blockId);
    }

    @Override
    public void deleteAll() {
        blockDao.deleteAll();
    }

    @Override
    public Transaction getTransaction(long transactionId) {
        return findTransaction(transactionId, Integer.MAX_VALUE);
    }

    @Override
    public Transaction findTransaction(long transactionId, int height) {
        TransactionalDataSource datasource = getDatasourceWithShardingByTransactionId(transactionId);
        return transactionDao.findTransaction(transactionId, height, datasource);
    }

    @Override
    public Transaction getTransactionByFullHash(String fullHash) {
        return findTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash) {
        return findTransactionByFullHash(fullHash, Integer.MAX_VALUE);
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash, int height) {
        TransactionalDataSource dataSource = getDatasourceWithShardingByTransactionId(Convert.fullHashToId(fullHash));
        return transactionDao.findTransactionByFullHash(fullHash, height, dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransaction(long transactionId) {
        return transactionDao.hasTransaction(transactionId, databaseManager.getDataSource()) || transactionIndexDao.getByTransactionId(transactionId) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransaction(long transactionId, int height) {
        boolean hasTransaction = transactionDao.hasTransaction(transactionId, height, databaseManager.getDataSource());
        if (!hasTransaction) {
            Integer transactionHeight = transactionIndexDao.getTransactionHeightByTransactionId(transactionId);
            hasTransaction = transactionHeight != null && transactionHeight <= height;
        }
        return hasTransaction;
    }


    @Override
    @Transactional(readOnly = true)
    public boolean hasTransactionByFullHash(String fullHash) {
        return hasTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransactionByFullHash(byte[] fullHash) {
        return hasTransactionByFullHash(fullHash, Integer.MAX_VALUE);
    }

    private boolean hasShardTransactionByFullHash(byte[] fullHash, int height) {
        long id = Convert.fullHashToId(fullHash);
        TransactionIndex transactionIndex = transactionIndexDao.getByTransactionId(id);
        byte[] hash = getTransactionIndexFullHash(transactionIndex);
        return Arrays.equals(hash, fullHash)
                && transactionIndexDao.getTransactionHeightByTransactionId(id) <= height;
    }


    @Override
    @Transactional(readOnly = true)
    public boolean hasTransactionByFullHash(byte[] fullHash, int height) {
        return transactionDao.hasTransactionByFullHash(fullHash, height, databaseManager.getDataSource()) || hasShardTransactionByFullHash(fullHash, height);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTransactionHeight(byte[] fullHash, int heightLimit) {
        Transaction transaction = transactionDao.findTransactionByFullHash(fullHash, heightLimit, databaseManager.getDataSource());
        Integer txHeight = null;
        if (transaction != null) {
            txHeight = transaction.getHeight();
        } else if (hasShardTransactionByFullHash(fullHash, heightLimit)){
            txHeight = transactionIndexDao.getTransactionHeightByTransactionId(Convert.fullHashToId(fullHash));
        }
        return txHeight;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getFullHash(long transactionId) {
        byte[] fullHash = transactionDao.getFullHash(transactionId, databaseManager.getDataSource());
        if (fullHash == null) {
            TransactionIndex transactionIndex = transactionIndexDao.getByTransactionId(transactionId);
            fullHash = getTransactionIndexFullHash(transactionIndex);
        }
        return fullHash;
    }

    private byte[] getTransactionIndexFullHash(TransactionIndex transactionIndex) {
        byte[] fullHash = null;
        if (transactionIndex != null) {
            fullHash = Convert.toFullHash(transactionIndex.getTransactionId(), transactionIndex.getPartialTransactionHash());
        }
        return fullHash;
    }

    @Override
    public Transaction loadTransaction(Connection con, ResultSet rs) throws AplException.NotValidException {
        return transactionDao.loadTransaction(con, rs);
    }

    @Override
    public int getTransactionCount() {
        return transactionDao.getTransactionCount();
    }

    public Long getTransactionCount(TransactionalDataSource dataSource, int from, int to) {
        return transactionDao.getTransactionCount(dataSource, from, to);
    }

/*
    @Override
    public DbIterator<Transaction> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }
*/

/*
    @Override
    public DbIterator<Transaction> getTransactions(long accountId, byte type, byte subtype, int blockTimestamp,
                                                       boolean includeExpiredPrunable) {
        return getTransactions(
                accountId, 0, type, subtype,
                blockTimestamp, false, false, false,
                0, -1, includeExpiredPrunable, false, true);
    }
*/

    @Override
    public List<Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                                   int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                   int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate) {
        int height = numberOfConfirmations > 0 ? getHeight() - numberOfConfirmations : Integer.MAX_VALUE;
        int prunableExpiration = Math.max(0, propertiesHolder.INCLUDE_EXPIRED_PRUNABLE() && includeExpiredPrunable ?
                timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime() :
                timeService.getEpochTime() - blockchainConfig.getMinPrunableLifetime());
        int limit = to == Integer.MAX_VALUE ? Integer.MAX_VALUE : to - from + 1;
        List<Transaction> transactions = transactionDao.getTransactions(
                databaseManager.getDataSource(),
                accountId, numberOfConfirmations, type, subtype,
                blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
                from, to, includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);

        if (transactions.size() < limit) {
            boolean noTransactions = transactions.size() == 0;
            limit -= transactions.size();
            List<TransactionalDataSource> fullDatasources = ((ShardManagement) databaseManager).getFullDatasources();
            for (TransactionalDataSource dataSource : fullDatasources) {
                if (noTransactions && from != 0) {
                    from -= transactionDao.getTransactionCountByFilter(databaseManager.getDataSource(),
                            accountId, numberOfConfirmations, type, subtype,
                            blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
                            includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
                } else {
                    from = 0;
                }
                List<Transaction> foundTxs = transactionDao.getTransactions(
                        dataSource,
                        accountId, numberOfConfirmations, type, subtype,
                        blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
                        from, limit - 1, includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
                transactions.addAll(foundTxs);
                noTransactions = foundTxs.size() == 0;
                if (foundTxs.size() == limit) {
                    break;
                }
                limit -= foundTxs.size();
            }
        }

        return transactions;
    }

    @Override
    public List<Transaction> getBlockTransactions(long blockId) {
        TransactionalDataSource dataSource = getDataSourceWithSharding(blockId);
        return transactionDao.findBlockTransactions(blockId, dataSource);
    }

    @Override
    public boolean hasBlock(long blockId, int height) {
        return lastBlock.get().getId() == blockId || blockDao.hasBlock(blockId, height, databaseManager.getDataSource());
    }

    @Override
    public boolean hasBlockInShards(long blockId) {
        return hasBlock(blockId) || blockIndexDao.getByBlockId(blockId) != null;
    }

    @Override
    public DbIterator<Transaction> getTransactions(byte type, byte subtype, int from, int to) {
        return transactionDao.getTransactions(type, subtype, from, to);
    }

    @Override
    public int getTransactionCount(long accountId, byte type, byte subtype) {
        return transactionDao.getTransactionCount(accountId, type, subtype);
    }

    @Override
    public DbIterator<Transaction> getTransactions(Connection con, PreparedStatement pstmt) {
        return transactionDao.getTransactions(con, pstmt);
    }

    @Override
    public List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp) {
        return transactionDao.findPrunableTransactions(con, minTimestamp, maxTimestamp);
    }


    @Override
    public Set<Long> getBlockGenerators(int limit) {
        int startHeight = getHeight() - MAX_BLOCK_GENERATOR_OFFSET;
        return blockDao.getBlockGenerators(startHeight, limit);
    }

    @Override
    public List<TransactionDbInfo> getTransactionsBeforeHeight(int height) {
        return transactionDao.getTransactionsBeforeHeight(height);
    }

    private TransactionalDataSource getDataSourceWithSharding(long blockId) {
        Long shardId = blockIndexDao.getShardIdByBlockId(blockId);
        return getShardDataSourceOrDefault(shardId);
    }

    private TransactionalDataSource getShardDataSourceOrDefault(Long shardId) {
        TransactionalDataSource dataSource = null;
        if (shardId != null) {
            dataSource = ((ShardManagement) databaseManager).getOrInitFullShardDataSourceById(shardId);
        }
        if (dataSource == null) {
            dataSource = databaseManager.getDataSource();
        }
        return dataSource;

    }

    private TransactionalDataSource getDataSourceWithShardingByHeight(int blockHeight) {
        Long shardId = blockIndexDao.getShardIdByBlockHeight(blockHeight);
        log.trace("Found shardId {} for block height {}", shardId, blockHeight);
        return getShardDataSourceOrDefault(shardId);
    }

    private TransactionalDataSource getDatasourceWithShardingByTransactionId(long transactionId) {
        Long shardId = transactionIndexDao.getShardIdByTransactionId(transactionId);
        return getShardDataSourceOrDefault(shardId);
    }

}
