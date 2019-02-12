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

import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.Constants;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.apollocurrency.aplwallet.apl.core.app.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.ReadWriteUpdateLock;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

@Singleton
public class BlockchainImpl implements Blockchain {

    private final BlockDao blockDao = CDI.current().select(BlockDaoImpl.class).get();
    private final TransactionDao transactionDao = CDI.current().select(TransactionDaoImpl.class).get();
    private final BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static volatile Time.EpochTime timeService = CDI.current().select(Time.EpochTime.class).get();
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();

    public BlockchainImpl() {}

    private final ReadWriteUpdateLock lock = new ReadWriteUpdateLock();
    private final AtomicReference<Block> lastBlock = new AtomicReference<>();

    @Override
    public void readLock() {
        lock.readLock().lock();
    }

    @Override
    public void readUnlock() {
        lock.readLock().unlock();
    }

    @Override
    public void updateLock() {
        lock.updateLock().lock();
    }

    @Override
    public void updateUnlock() {
        lock.updateLock().unlock();
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

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
    public Block getBlock(long blockId) {
        Block block = lastBlock.get();
        if (block.getId() == blockId) {
            return block;
        }
        return blockDao.findBlock(blockId);
    }

    @Override
    public boolean hasBlock(long blockId) {
        return lastBlock.get().getId() == blockId || blockDao.hasBlock(blockId);
    }

    @Override
    public DbIterator<Block> getAllBlocks() {
        return blockDao.getAllBlocks();
    }

    @Override
    public DbIterator<Block> getBlocks(int from, int to) {
        int blockchainHeight = getHeight();
        int calculatedFrom = blockchainHeight - from;
        int calculatedTo = blockchainHeight - to;
        return blockDao.getBlocks(calculatedFrom, calculatedTo);
    }

    @Override
    public DbIterator<Block> getBlocks(long accountId, int timestamp) {
        return getBlocks(accountId, timestamp, 0, -1);
    }

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
        return blockDao.loadBlock(con, rs, loadTransactions);
    }

    @Override
    public void saveBlock(Connection con, Block block) {
        blockDao.saveBlock(con, block);
    }

    @Override
    public void commit(Block block) {
        blockDao.commit(block);
    }

    @Override
    public int getBlockCount(long accountId) {
        return blockDao.getBlockCount(accountId);
    }

    @Override
    public DbIterator<Block> getBlocks(Connection con, PreparedStatement pstmt) {
        return blockDao.getBlocks(con, pstmt);
    }

    @Override
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        // Check the block cache
        List<Long> result = new ArrayList<>(blockDao.getBlockCacheSize());
        synchronized(blockDao.getBlockCache()) {
            Block block = blockDao.getBlockCache().get(blockId);
            if (block != null) {
                Collection<Block> cacheMap = blockDao.getHeightMap().tailMap(block.getHeight() + 1).values();
                for (Block cacheBlock : cacheMap) {
                    if (result.size() >= limit) {
                        break;
                    }
                    result.add(cacheBlock.getId());
                }
                return result;
            }
        }
        return blockDao.getBlockIdsAfter(blockId, limit, result);
    }

    @Override
    public List<Block> getBlocksAfter(long blockId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        // Check the block cache
        List<Block> result = new ArrayList<>(blockDao.getBlockCacheSize());
        synchronized(blockDao.getBlockCache()) {
            Block block = blockDao.getBlockCache().get(blockId);
            if (block != null) {
                Collection<Block> cacheMap = blockDao.getHeightMap().tailMap(block.getHeight() + 1).values();
                for (Block cacheBlock : cacheMap) {
                    if (result.size() >= limit) {
                        break;
                    }
                    result.add(cacheBlock);
                }
                return result;
            }
        }
        return blockDao.getBlocksAfter(blockId, limit, result);
    }

    @Override
    public List<Block> getBlocksAfter(long blockId, List<Long> blockList) {
        if (blockList.isEmpty()) {
            return Collections.emptyList();
        }
        // Check the block cache
        List<Block> result = new ArrayList<>(blockDao.getBlockCacheSize());
        synchronized(blockDao.getBlockCache()) {
            Block block = blockDao.getBlockCache().get(blockId);
            if (block != null) {
                Collection<Block> cacheMap = blockDao.getHeightMap().tailMap(block.getHeight() + 1).values();
                int index = 0;
                for (Block cacheBlock : cacheMap) {
                    if (result.size() >= blockList.size() || cacheBlock.getId() != blockList.get(index++)) {
                        break;
                    }
                    result.add(cacheBlock);
                }
                return result;
            }
        }
        return blockDao.getBlocksAfter(blockId, blockList, result);
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
        return blockDao.findBlockIdAtHeight(height);
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
        return blockDao.findBlockAtHeight(height);
    }

    @Override
    public Block getECBlock(int timestamp) {
        Block block = getLastBlock(timestamp);
        if (block == null) {
            return getBlockAtHeight(0);
        }
        return blockDao.findBlockAtHeight(Math.max(block.getHeight() - 720, 0));
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
    public Map<Long, Transaction> getTransactionCache() {
        return blockDao.getTransactionCache();
    }

    @Override
    public Transaction getTransaction(long transactionId) {
        return transactionDao.findTransaction(transactionId);
    }

    @Override
    public Transaction findTransaction(long transactionId, int height) {
        return transactionDao.findTransaction(transactionId, height);
    }

    @Override
    public Transaction getTransactionByFullHash(String fullHash) {
        return transactionDao.findTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash) {
        return transactionDao.findTransactionByFullHash(fullHash);
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash, int height) {
        return transactionDao.findTransactionByFullHash(fullHash, height);
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        return transactionDao.hasTransaction(transactionId);
    }

    @Override
    public boolean hasTransaction(long transactionId, int height) {
        return transactionDao.hasTransaction(transactionId, height);
    }

    @Override
    public boolean hasTransactionByFullHash(String fullHash) {
        return transactionDao.hasTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Override
    public boolean hasTransactionByFullHash(byte[] fullHash, int height) {
        return transactionDao.hasTransactionByFullHash(fullHash, height);
    }

    @Override
    public byte[] getFullHash(long transactionId) {
        return transactionDao.getFullHash(transactionId);
    }

    @Override
    public Transaction loadTransaction(Connection con, ResultSet rs) throws AplException.NotValidException {
        return transactionDao.loadTransaction(con, rs);
    }

    @Override
    public int getTransactionCount() {
        return transactionDao.getTransactionCount();
    }

    @Override
    public DbIterator<Transaction> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    @Override
    public DbIterator<Transaction> getTransactions(long accountId, byte type, byte subtype, int blockTimestamp,
                                                       boolean includeExpiredPrunable) {
        return getTransactions(
                accountId, 0, type, subtype,
                blockTimestamp, false, false, false,
                0, -1, includeExpiredPrunable, false, true);
    }

    @Override
    public DbIterator<Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                                       int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                       int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate) {

        int height = numberOfConfirmations > 0 ? getHeight() - numberOfConfirmations : Integer.MAX_VALUE;
        int prunableExpiration = Math.max(0, propertiesHolder.INCLUDE_EXPIRED_PRUNABLE() && includeExpiredPrunable ?
                timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime() :
                timeService.getEpochTime() - blockchainConfig.getMinPrunableLifetime());

        return transactionDao.getTransactions(
                accountId, numberOfConfirmations, type, subtype,
                blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
                from, to, includeExpiredPrunable, executedOnly, includePrivate, height, prunableExpiration);
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

    //phased transactions
    @Override
    public List<Transaction> getExpectedTransactions(Filter<Transaction> filter) {
        Map<TransactionType, Map<String, Integer>> duplicates = new HashMap<>();
        BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        List<Transaction> result = new ArrayList<>();
        readLock();
        try {
            try (DbIterator<Transaction> phasedTransactions = PhasingPoll.getFinishingTransactions(getHeight() + 1)) {
                for (Transaction phasedTransaction : phasedTransactions) {
                    try {
                        phasedTransaction.validate();
                        if (!phasedTransaction.attachmentIsDuplicate(duplicates, false) && filter.test(phasedTransaction)) {
                            result.add(phasedTransaction);
                        }
                    } catch (AplException.ValidationException ignore) {
                    }
                }
            }

            blockchainProcessor.selectUnconfirmedTransactions(duplicates, getLastBlock(), -1).forEach(
                    unconfirmedTransaction -> {
                        Transaction transaction = unconfirmedTransaction.getTransaction();
                        if (transaction.getPhasing() == null && filter.test(transaction)) {
                            result.add(transaction);
                        }
                    }
            );
        } finally {
            readUnlock();
        }
        return result;
    }

    @Override
    public DbIterator<Transaction> getReferencingTransactions(long transactionId, int from, int to) {
        return transactionDao.getReferencingTransactions(transactionId, from, to);
    }

    @Override
    public Set<Long> getBlockGenerators(int startHeight) {
        return blockDao.getBlockGenerators(startHeight);
    }

}
