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
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TransactionIndex;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class BlockchainImpl implements Blockchain {
    /**
     * Specify offset from current height to retrieve block generators [currentHeight - offset; currentHeight] for further tracking generator hitTime
     */
    private static final int MAX_BLOCK_GENERATOR_OFFSET = 10_000;
    private final BlockDao blockDao;
    private final TransactionService transactionService;
    private final TransactionDao transactionDao;
    private final TimeService timeService;
    private final TransactionIndexDao transactionIndexDao;
    private final BlockIndexService blockIndexService;
    private final DatabaseManager databaseManager;
    private final ShardDao shardDao;
    private final ShardRecoveryDao shardRecoveryDao;
    private final PrunableLoadingService prunableService;
    private final PublicKeyDao publicKeyDao;

    private final AtomicReference<Block> lastBlock;
    private final AtomicReference<Block> shardInitialBlock;

    @Inject
    public BlockchainImpl(BlockDao blockDao,
                          TransactionService transactionService,
                          TransactionDao transactionDao,
                          TimeService timeService,
                          TransactionIndexDao transactionIndexDao,
                          BlockIndexService blockIndexService,
                          DatabaseManager databaseManager,
                          ShardDao shardDao,
                          ShardRecoveryDao shardRecoveryDao,
                          PrunableLoadingService prunableService,
                          PublicKeyDao publicKeyDao) {
        this.blockDao = blockDao;
        this.transactionService = transactionService;
        this.transactionDao = transactionDao;
        this.timeService = timeService;
        this.transactionIndexDao = transactionIndexDao;
        this.blockIndexService = blockIndexService;
        this.databaseManager = databaseManager;
        this.shardDao = shardDao;
        this.shardRecoveryDao = shardRecoveryDao;
        this.prunableService = prunableService;
        this.publicKeyDao = publicKeyDao;
        this.lastBlock = new AtomicReference<>();
        this.shardInitialBlock = new AtomicReference<>();
    }

    @Override
    public Block getLastBlock() {
        return lastBlock.get();
    }

    @Override
    public void setLastBlock(Block block) {
        lastBlock.set(block);
    }

    @PostConstruct
    @Override
    public void update() {
        this.lastBlock.set(findLastBlock());
        this.shardInitialBlock.set(findFirstBlock());
        ((ShardManagement) this.databaseManager).initFullShards(
            shardDao.getAllCompletedShards().stream().map(Shard::getShardId).collect(Collectors.toList()));
    }

    @Override
    public int getHeight() {
        Block last = lastBlock.get();
        return last == null ? 0 : last.getHeight();
    }

    @Transactional(readOnly = true)
    @Override
    public int getLastBlockTimestamp() {
        Block last = lastBlock.get();
        return last == null ? 0 : last.getTimestamp();
    }

    @Transactional(readOnly = true)
    @Override
    public Block getLastBlock(int timestamp) {
        Block block = lastBlock.get();
        if (timestamp >= block.getTimestamp()) {
            return block;
        }
        return loadBlockData(blockDao.findLastBlock(timestamp));
    }

    @Override
    @Transactional(readOnly = true)
    public Block getBlock(long blockId) {
        Block block = lastBlock.get();
        if (block.getId() == blockId) {
            return block;
        }
        TransactionalDataSource dataSource = blockIndexService.getDataSourceWithSharding(blockId);

        return loadBlockData(blockDao.findBlock(blockId, dataSource));
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Block> getBlocks(int from, int to, int timestamp) {
        int blockchainHeight = getHeight();
        int calculatedFrom = blockchainHeight - from;
        int calculatedTo = blockchainHeight - to;
        return loadBlockData(CollectionUtil.toList(blockDao.getBlocks(null, calculatedFrom, calculatedTo, timestamp)));
    }

    /**
     * Retrieve records from correct shard AND main database.
     *
     * @param from      height index
     * @param to        height index
     * @param timestamp optional block timestamp
     * @return composed block's stream
     */
    @Transactional(readOnly = true)
    @Override
    public List<Block> getBlocksFromShards(int from, int to, int timestamp) {
        int blockchainHeight = getHeight();
        int calculatedFrom = blockchainHeight - from;
        int calculatedTo = blockchainHeight - to;
        int totalToFetch = to - from;
        log.trace("start getBlocksStream( from={} / {}, to={} / {}, timestamp={} ): , currentHeight={}, totalToFetch={}",
            from, calculatedFrom, to, calculatedTo, timestamp, blockchainHeight, totalToFetch);
        List<Block> allSourcesList = new ArrayList<>(); // complete list from all sources
        // select possibly - none, one, two shard's records by specified height range
        List<Shard> foundShards = shardDao.getCompletedBetweenBlockHeight(calculatedTo, calculatedFrom); // reverse params
        log.trace("getBlocksStream( from={}, to={} ): foundShards=[{}] / shardIds={}, currentHeight={}",
            calculatedFrom, calculatedTo, foundShards.size(), foundShards.stream().map(Shard::getShardId).collect(Collectors.toList()), blockchainHeight);
        if (foundShards.size() == 0) {
            // select blocks from main database only
            DbIterator<Block> iterator = blockDao.getBlocks(null, calculatedFrom, calculatedTo, timestamp);
            allSourcesList.addAll(CollectionUtil.toList(iterator));
        } else {
            // loop over ONE or SEVERAL available shards
            for (Shard shard: foundShards) {
                // get shard data source trying to fetch records
                TransactionalDataSource dataSource =
                    ((ShardManagement) databaseManager).getOrInitFullShardDataSourceById(shard.getShardId());
                // make select on blocks from shard
                log.trace("getBlocksStream -> getBlocks( from={}, to={} ): shardIds={}",
                    calculatedFrom, calculatedTo, dataSource.getDbIdentity());
                DbIterator<Block> iterator = blockDao.getBlocks(dataSource, calculatedFrom, calculatedTo, timestamp);
                allSourcesList.addAll(CollectionUtil.toList(iterator));
            }
            // add possible blocks from main database (if any)
            DbIterator<Block> iterator = blockDao.getBlocks(null, calculatedFrom, calculatedTo, timestamp);
            allSourcesList.addAll(CollectionUtil.toList(iterator));
        }
        log.trace("DONE getBlocksStream( from={}, to={} ): foundShards=[{}] / shardIds={}, currentHeight={}",
            calculatedFrom, calculatedTo, foundShards.size(), foundShards.stream().map(Shard::getShardId).collect(Collectors.toList()), blockchainHeight);
        return loadBlockData(allSourcesList);
    }

    @Transactional
    @Override
    public Block findFirstBlock() {
        return loadBlockData(blockDao.findFirstBlock());
    }

    private Block loadBlockData(Block block) {
        if (block == null) {
            return null;
        }
        PublicKey publicKey = publicKeyDao.searchAll(block.getGeneratorId());
        if (publicKey != null) {
            block.setGeneratorPublicKey(publicKey.getPublicKey());
        } else {
            //special case when scan was failed and no public keys in db exist
            log.warn("No public key for generator's account {} on block {} at {}", block.getGeneratorId(), block.getId(), block.getHeight());
        }
        return block;
    }

    private List<Block> loadBlockData(List<Block> blocks) {
        blocks.forEach(this::loadBlockData);
        return blocks;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Block> getBlocksByAccount(long accountId, int from, int to, int timestamp) {
        return loadBlockData(CollectionUtil.toList(blockDao.getBlocksByAccount(null, accountId, from, to, timestamp)));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Block> getBlocksByAccountFromShards(long accountId, int from, int to, int timestamp) {
        long start = System.currentTimeMillis();
        int totalToFetch = to - from;
        log.trace("start getBlocksByAccountStream, accountId = {}, timestamp={}, from={}, to={}, in total={}",
            accountId, timestamp, from, to, totalToFetch);
        List<Block> finalResult = new ArrayList<>(totalToFetch);
        DbIterator<Block> iterator = blockDao.getBlocksByAccount(null, accountId, from, to, timestamp); // fetch from main db
        finalResult.addAll(CollectionUtil.toList(iterator));
        log.trace("getBlocksByAccountStream from main db, accountId = {}, timestamp={}, from={}, to={} in {} ms",
            accountId, timestamp, from, to, System.currentTimeMillis() - start);
        if (finalResult.size() >= totalToFetch) {
            return finalResult;
        }
        int nextTo = to - finalResult.size(); // decrease upper limit
        Iterator<TransactionalDataSource> fullDataSources = ((ShardManagement) databaseManager).getAllFullDataSourcesIterator();
        // loop shards
        while (fullDataSources.hasNext()) {
            long startShard = System.currentTimeMillis();
            TransactionalDataSource dataSource = fullDataSources.next();
            iterator = blockDao.getBlocksByAccount(dataSource, accountId, from, nextTo, timestamp);
            List<Block> fetchedFromShard = CollectionUtil.toList(iterator);
            log.trace("getBlocksByAccountStream, accountId = {} from shard db[{}] = fetched [{}] in {} ms", accountId, dataSource.getDbIdentity(),
                fetchedFromShard.size(), (System.currentTimeMillis() - startShard) );
            nextTo -= fetchedFromShard.size(); // decrease upper limit
            finalResult.addAll(fetchedFromShard);
            if (finalResult.size() >= totalToFetch + 1) {
                log.trace("getBlocksByAccountStream STOP loop from shard={}, accountId = {}, timestamp={}, from={}, nextTo={} in {} ms",
                    dataSource.getDbIdentity(), accountId, timestamp, from, nextTo, System.currentTimeMillis() - startShard);
                break;
            }
        }
        log.trace("DONE getBlocksByAccountStream[{}], accountId = {}, timestamp={}, from={}, to={} in {} ms",
            finalResult.size(), accountId, timestamp, from, to, System.currentTimeMillis() - start);
        return loadBlockData(finalResult);
    }

    @Transactional(readOnly = true)
    @Override
    public Block findLastBlock() {
        return loadBlockData(blockDao.findLastBlock());
    }

    @Override
    @Transactional(readOnly = true)
    public Block loadBlock(Connection con, ResultSet rs, boolean loadTransactions) {
        Block block = loadBlockData(blockDao.loadBlock(con, rs));
        if (loadTransactions) {
            List<Transaction> blockTransactions = this.getOrLoadTransactions(block);
            block.setTransactions(blockTransactions);
        }
        return block;
    }

    @Transactional
    @Override
    public void saveBlock(Block block) {
        blockDao.saveBlock(block);
        transactionService.saveTransactions(this.getOrLoadTransactions(block));
    }

    public List<Transaction> getOrLoadTransactions(Block parentBlock) {
        if (parentBlock.getTransactions() == null || parentBlock.getTransactions().size() == 0) {
            List<Transaction> blockTransactions = this.getBlockTransactions(parentBlock.getId());
            if (blockTransactions.size() > 0) {
                List<Transaction> transactions = Collections.unmodifiableList(blockTransactions);
                short index = 0;
                for (Transaction transaction : transactions) {
                    transaction.setBlock(parentBlock);
                    transaction.setIndex(index++);
                    transaction.bytes();
                    prunableService.loadTransactionPrunables(transaction);
                }
                parentBlock.setTransactions(transactions);
            } else {
                parentBlock.setTransactions(Collections.emptyList());
            }
        } else if (parentBlock.getTransactions() == null) {
            parentBlock.setTransactions(Collections.emptyList());
        }
        return parentBlock.getTransactions();
    }

    @Transactional
    @Override
    public void commit(Block block) {
        blockDao.commit(block);
    }

    @Override
    public Long getBlockCount(TransactionalDataSource dataSource, int from, int to) {
        return blockDao.getBlockCount(dataSource, from, to);
    }

    @Transactional(readOnly = true)
    @Override
    public int getBlockCount(long accountId) {
        long start = System.currentTimeMillis();
        log.trace("start getBlockCount, accountId = {}", accountId);
        int totalCount = blockDao.getBlockCount(null, accountId); // fetch count from main db
        log.trace("getBlockCount, accountId = {} from main db = {} in {} ms", accountId, totalCount, (System.currentTimeMillis() - start) );
        Iterator<TransactionalDataSource> fullDataSources = ((ShardManagement) databaseManager).getAllFullDataSourcesIterator();
        // loop shards
        while (fullDataSources.hasNext()) {
            long startShard = System.currentTimeMillis();
            TransactionalDataSource dataSource = fullDataSources.next();
            int blockCountInShard = blockDao.getBlockCount(dataSource, accountId);
            totalCount += blockCountInShard; // fetch more count values from shard db
            log.trace("getBlockCount, accountId = {} from shard db[{}] = {} in {} ms", accountId, dataSource.getDbIdentity(),
                blockCountInShard, (System.currentTimeMillis() - startShard) );
        }
        log.trace("DONE, accountId = {} from all dbs = {} in {} ms", accountId, totalCount, (System.currentTimeMillis() - start) );
        return totalCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBlockIdsAfter(long blockId, int limit) {
        // Check the block cache

        List<Long> result = new ArrayList<>(limit);

        Integer height = blockIndexService.getHeight(blockId);
        if (height != null) {
            result.addAll(blockIndexService.getBlockIdsAfter(height, limit));
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
        Integer height = blockIndexService.getHeight(blockId);
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
    @Transactional(readOnly = true)
    public List<Block> getBlocksAfter(long blockId, List<Long> blockIdList) {
        // Check the block cache
        if (blockIdList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Block> result = new ArrayList<>();
        TransactionalDataSource dataSource;
//        long time = System.currentTimeMillis();
        Integer fromBlockHeight = getBlockHeight(blockId);
        if (fromBlockHeight != null) {
            int prevSize;
            do {
                dataSource = blockIndexService.getDataSourceWithShardingByHeight(fromBlockHeight + 1); //should return datasource, where such block exist or default datasource
//                log.info("Datasource - {}", dataSource.getUrl());
                prevSize = result.size();
                try (Connection con = dataSource.getConnection()) { //get blocks and transactions in one connectiщn
                    blockDao.getBlocksAfter(fromBlockHeight, blockIdList, result, con, prevSize);
                    for (int i = prevSize; i < result.size(); i++) {
                        Block block = result.get(i);
                        List<Transaction> blockTransactions = this.getOrLoadTransactions(block);
                        block.setTransactions(blockTransactions);
                    }
                    if (result.size() - 1 >= 0) {
                        fromBlockHeight = getBlockHeight(blockIdList.get(result.size() - 1));
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            } while (result.size() != prevSize && dataSource != databaseManager.getDataSource() && blockIndexService.getDataSourceWithShardingByHeight(fromBlockHeight + 1) != dataSource);
        }
//        log.info("GetAfterBlock time {}", System.currentTimeMillis() - time);
        return loadBlockData(result);
    }

    @Transactional(readOnly = true)
    @Override
    public long getBlockIdAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block.getId();
        }
        BlockIndex blockIndex = blockIndexService.getByBlockHeight(height);
        if (blockIndex != null) {
            return blockIndex.getBlockId();
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        return blockDao.findBlockIdAtHeight(height, dataSource);
    }

    @Transactional(readOnly = true)
    @Override
    public Block getBlockAtHeight(int height) {
        Block block = lastBlock.get();
        if (height > block.getHeight()) {
            throw new IllegalArgumentException("Invalid height " + height + ", current blockchain is at " + block.getHeight());
        }
        if (height == block.getHeight()) {
            return block;
        }
        TransactionalDataSource dataSource = blockIndexService.getDataSourceWithShardingByHeight(height);
        return blockDao.findBlockAtHeight(height, dataSource);
    }

    @Override
    public Block getShardInitialBlock() {
        return shardInitialBlock.get();
    }

    @Override
    public void setShardInitialBlock(Block block) {
        shardInitialBlock.set(block);
    }

    @Transactional(readOnly = true)
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

    @Transactional
    @Override
    public void deleteBlocksFromHeight(int height) {
        log.debug("deleteBlocksFromHeight ({})", height);
        blockDao.deleteBlocksFromHeight(height);
    }

    @Transactional
    @Override
    public Block deleteBlocksFrom(long blockId) {
        return loadBlockData(blockDao.deleteBlocksFrom(blockId));
    }

    @Override
    @Transactional
    public void deleteAll() {
        log.debug("started deleteAll()...");
        blockDao.deleteAll(); // delete both : blocks + transactions
        shardRecoveryDao.hardDeleteAllShardRecovery();
        shardDao.hardDeleteAllShards();
        transactionIndexDao.hardDeleteAllTransactionIndex();
        blockIndexService.hardDeleteAllBlockIndex();
        log.debug("finished deleteAll()");
    }

    @Transactional(readOnly = true)
    @Override
    public Transaction getTransaction(long transactionId) {
        return findTransaction(transactionId, Integer.MAX_VALUE);
    }

    @Override
    public Transaction findTransaction(long transactionId, int height) {
        //cross sharding
        return loadPrunable(transactionService.findTransaction(transactionId, height));
    }

    @Transactional(readOnly = true)
    @Override
    public Transaction getTransactionByFullHash(String fullHash) {
        return findTransactionByFullHash(Convert.parseHexString(fullHash));
    }

    @Transactional(readOnly = true)
    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash) {
        return findTransactionByFullHash(fullHash, Integer.MAX_VALUE);
    }

    @Override
    public Transaction findTransactionByFullHash(byte[] fullHash, int height) {
        //cross sharding
        return loadPrunable(transactionService.findTransactionByFullHash(fullHash, height));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransaction(long transactionId) {
        return transactionService.hasTransaction(transactionId) ||
            transactionIndexDao.countByTransactionId(transactionId) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransaction(long transactionId, int height) {
        boolean hasTransaction = transactionService.hasTransaction(transactionId, height);
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
        return transactionService.hasTransactionByFullHash(fullHash, height) || hasShardTransactionByFullHash(fullHash, height);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTransactionHeight(byte[] fullHash, int heightLimit) {
        Transaction transaction = transactionService.findTransactionByFullHash(fullHash, heightLimit);
        Integer txHeight = null;
        if (transaction != null) {
            txHeight = transaction.getHeight();
        } else if (hasShardTransactionByFullHash(fullHash, heightLimit)) {
            txHeight = transactionIndexDao.getTransactionHeightByTransactionId(Convert.fullHashToId(fullHash));
        }
        return txHeight;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getFullHash(long transactionId) {
        byte[] fullHash = transactionService.getFullHash(transactionId);
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

    @Transactional(readOnly = true)
    @Override
    public int getTransactionCount() {
        return transactionService.getTransactionCount();
    }

    public Long getTransactionCount(TransactionalDataSource dataSource, int from, int to) {
        return transactionDao.getTransactionCount(dataSource, from, to);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                             int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                             int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate) {

        return transactionService.getTransactionsCrossShardingByAccount(accountId, getHeight(), numberOfConfirmations, type, subtype,
            blockTimestamp, withMessage, phasedOnly, nonPhasedOnly,
            from, to, includeExpiredPrunable, executedOnly, includePrivate);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Transaction> getBlockTransactions(long blockId) {
        //cross sharding
        return transactionService.findBlockTransactions(blockId);
    }

    @Transactional(readOnly = true)
    @Override
    public long getBlockTransactionCount(long blockId) {
        //cross sharding
        return transactionService.getBlockTransactionsCount(blockId);
    }

    @Override
    public boolean isInitialized() {
        return getLastBlock() != null && getShardInitialBlock() != null;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasBlock(long blockId, int height) {
        return lastBlock.get().getId() == blockId || blockDao.hasBlock(blockId, height, databaseManager.getDataSource());
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasBlockInShards(long blockId) {
        return hasBlock(blockId) || blockIndexService.getByBlockId(blockId) != null;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Transaction> getTransactions(byte type, byte subtype, int from, int to) {
        return transactionService.getTransactions(type, subtype, from, to);
    }

    @Transactional(readOnly = true)
    @Override
    public int getTransactionCount(long accountId, byte type, byte subtype) {
        return transactionService.getTransactionCount(accountId, type, subtype);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp) {
        return transactionDao.findPrunableTransactions(con, minTimestamp, maxTimestamp);
    }

    @Transactional(readOnly = true)
    @Override
    public Set<Long> getBlockGenerators(int limit) {
        int startHeight = getHeight() - MAX_BLOCK_GENERATOR_OFFSET;
        return blockDao.getBlockGenerators(startHeight, limit);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TransactionDbInfo> getTransactionsBeforeHeight(int height) {
        return transactionService.getTransactionsBeforeHeight(height);
    }

    @Override
    public boolean hasConfirmations(long id, int confirmations) {
        return hasTransaction(id, getHeight() - confirmations);
    }

    @Override
    public boolean isExpired(Transaction tx) {
        return timeService.getEpochTime() > tx.getExpiration();
    }

    private Transaction loadPrunable(Transaction transaction) {
        if (transaction != null) {
            prunableService.loadTransactionPrunables(transaction);
        }
        return transaction;
    }

    @Override
    public List<Transaction> loadPrunables(List<Transaction> transactions) {
        transactions.forEach(this::loadPrunable);
        return transactions;
    }

}
