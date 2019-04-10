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

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;

import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import org.slf4j.Logger;

@Singleton
public class BlockDaoImpl implements BlockDao {
    private static final Logger LOG = getLogger(BlockDaoImpl.class);

    /** Block cache */

    private static final int DEFAULT_BLOCK_CACHE_SIZE = 10;
    private int blockCacheSize;
    private final Map<Long, Block> blockCache;
    private final SortedMap<Integer, Block> heightMap;
    private final Map<Long, Transaction> transactionCache;
    private final DerivedTablesRegistry tablesRegistry;
    private DatabaseManager databaseManager;
    private TransactionDao transactionDao;
    private BlockIndexDao blockIndexDao;


    public BlockDaoImpl(int blockCacheSize, Map<Long, Block> blockCache, SortedMap<Integer, Block> heightMap,
                        Map<Long, Transaction> transactionCache, DerivedTablesRegistry tablesRegistry, DatabaseManager databaseManager) {
        this.blockCacheSize = blockCacheSize;
        this.blockCache = blockCache == null ? new HashMap<>() : blockCache;
        this.heightMap = heightMap == null ? new TreeMap<>() : heightMap;
        this.transactionCache = transactionCache == null ? new HashMap<>() : transactionCache;
        this.tablesRegistry = Objects.requireNonNull(tablesRegistry, "Derived table registry cannot be null");
        this.databaseManager = Objects.requireNonNull(databaseManager, "DatabaseManager cannot be null");
    }

    @Inject
    public BlockDaoImpl(DerivedTablesRegistry derivedDbTablesRegistry, DatabaseManager databaseManager) {
        this(DEFAULT_BLOCK_CACHE_SIZE, new HashMap<>(), new TreeMap<>(), new HashMap<>(), derivedDbTablesRegistry, databaseManager);
    }

    private TransactionDao lookupTransactionDao() {
        if (transactionDao == null) {
            this.transactionDao = CDI.current().select(TransactionDaoImpl.class).get();
        }
        return transactionDao;
    }

    private BlockIndexDao lookupBlockIndexDao() {
        if (blockIndexDao == null) {
            this.blockIndexDao = CDI.current().select(BlockIndexDao.class).get();
        }
        return blockIndexDao;
    }

    private void clearBlockCache() {
        synchronized (blockCache) {
            blockCache.clear();
            heightMap.clear();
            transactionCache.clear();
        }
    }

    @Override
    public Block findBlock(long blockId) {
        // Check the block cache
        synchronized (blockCache) {
            Block block = blockCache.get(blockId);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        TransactionalDataSource dataSource = getDataSourceWithSharding(blockId);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                Block block = null;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private TransactionalDataSource getDataSourceWithSharding(long blockId) {
//        databaseManager.getDataSource();
        TransactionalDataSource dataSource;
        Long shardId = lookupBlockIndexDao().getShardIdByBlockId(blockId);
        if (shardId != null) {
            // shard data source
            dataSource = ((ShardManagement)databaseManager).getOrCreateShardDataSourceById(shardId);
        } else {
            // default data source
            dataSource = databaseManager.getDataSource();
        }
        return dataSource;
    }

    private TransactionalDataSource getDataSourceWithShardingByHeight(int blockHeight) {
//        databaseManager.getDataSource();
        TransactionalDataSource dataSource;
        Long shardId = lookupBlockIndexDao().getShardIdByBlockHeight(blockHeight);
        if (shardId != null) {
            // shard data source
            dataSource = ((ShardManagement)databaseManager).getOrCreateShardDataSourceById(shardId);
        } else {
            // default data source
            dataSource = databaseManager.getDataSource();
        }
        return dataSource;
    }

    @Override
    public boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE);
    }

    @Override
    public boolean hasBlock(long blockId, int height) {
        // Check the block cache
        synchronized(blockCache) {
            Block block = blockCache.get(blockId);
            if (block != null) {
                return block.getHeight() <= height;
            }
        }
        // Search the database
        TransactionalDataSource dataSource = getDataSourceWithSharding(blockId);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT height FROM block WHERE id = ? AND (next_block_id <> 0 OR next_block_id IS NULL)")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public long findBlockIdAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            Block block = heightMap.get(height);
            if (block != null) {
                return block.getId();
            }
        }
        // Search the database
        TransactionalDataSource dataSource = getDataSourceWithShardingByHeight(height);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Map<Long, Block> getBlockCache() {
        return blockCache;
    }

    @Override
    public SortedMap<Integer, Block> getHeightMap() {
        return heightMap;
    }

    @Override
    public Block findBlockAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            Block block = heightMap.get(height);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        TransactionalDataSource dataSource = getDataSourceWithShardingByHeight(height);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                Block block;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                } else {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int getBlockCacheSize() {
        return blockCacheSize;
    }

    @Override
    public Map<Long, Transaction> getTransactionCache() {
        return transactionCache;
    }

    @Override
    public Block findLastBlock() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT * FROM block WHERE next_block_id <> 0 OR next_block_id IS NULL ORDER BY timestamp DESC LIMIT 1")) {
            Block block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

/*
    @Override
    public DbIterator<Block> getAllBlocks() {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
*/

    @Override
    public List<byte[]> getBlockSignaturesFrom(int fromHeight, int toHeight) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        List<byte[]> blockSignatures = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT block_signature FROM block "
                     + "WHERE height >= ? AND height < ? ")) {
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    blockSignatures.add(rs.getBytes("block_signature"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return blockSignatures;
    }

    @Override
    public DbIterator<Block> getBlocks(Connection con, PreparedStatement pstmt) {
        return new DbIterator<>(con, pstmt, this::loadBlock);
    }

    @Override
    public DbIterator<Block> getBlocks(long accountId, int timestamp, int from, int to) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE generator_id = ? "
                    + (timestamp > 0 ? " AND timestamp >= ? " : " ") + "ORDER BY height DESC"
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            if (timestamp > 0) {
                pstmt.setInt(++i, timestamp);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbIterator<Block> getBlocks(int from, int to) {
        Connection con = null;
        TransactionalDataSource dataSource = databaseManager.getDataSource(); // TODO: YL implement partial fetch from main + shard db
        try {
            con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height <= ? AND height >= ? ORDER BY height DESC");
            pstmt.setInt(1, from);
            pstmt.setInt(2, to);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Long getBlockCount(int from, int to) {
        // select from main db
        return getBlockCount(null, from, to);
    }

    @Override
    public Long getBlockCount(TransactionalDataSource dataSource, int from, int to) {
        if (dataSource == null) {
            // select from main db
            dataSource = databaseManager.getDataSource();
        }
        try (Connection con = dataSource.getConnection()) {
            PreparedStatement pstmt = con.prepareStatement("SELECT count(*) as blockCount FROM block WHERE height >= ? AND height < ?");
            pstmt.setInt(1, from);
            pstmt.setInt(2, to);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("blockCount");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return 0L;
    }

    @Override
    public int getBlockCount(long accountId) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM block WHERE generator_id = ?")) {
            pstmt.setLong(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Long> getBlockIdsAfter(long blockId, int limit, List<Long> result) {
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block "
                     + "WHERE db_id > IFNULL ((SELECT db_id FROM block WHERE id = ?), " + Long.MAX_VALUE + ") "
                     + "ORDER BY db_id ASC LIMIT ?")) {
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public List<Block> getBlocksAfter(long blockId, int limit, List<Block> result) {
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                     + "WHERE db_id > IFNULL ((SELECT db_id FROM block WHERE id = ?), " + Long.MAX_VALUE + ") "
                     + "ORDER BY db_id ASC LIMIT ?")) {
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(this.loadBlock(con, rs, true));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public List<Block> getBlocksAfter(long blockId, List<Long> blockList, List<Block> result) {
        // Search the database
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                     + "WHERE db_id > IFNULL ((SELECT db_id FROM block WHERE id = ?), " + Long.MAX_VALUE + ") "
                     + "ORDER BY db_id ASC LIMIT ?")) {
            pstmt.setLong(1, blockId);
            pstmt.setInt(2, blockList.size());
            try (ResultSet rs = pstmt.executeQuery()) {
                int index = 0;
                while (rs.next()) {
                    Block block = this.loadBlock(con, rs, true);
                    if (block.getId() != blockList.get(index++)) {
                        break;
                    }
                    result.add(block);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public Block findBlockWithVersion(int skipCount, int version) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
             "SELECT * FROM block WHERE version = ? ORDER BY block_timestamp DESC LIMIT 1 OFFSET ?)")) {
            int i = 0;
            pstmt.setInt(++i, version);
            pstmt.setInt(++i, skipCount);
            Block block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Block findAdaptiveBlock(int skipCount) {
        return findBlockWithVersion(skipCount, Block.ADAPTIVE_BLOCK_VERSION);
    }

    @Override
    public Block findLastAdaptiveBlock() {
        return findAdaptiveBlock(0);
    }

    @Override
    public Block findInstantBlock(int skipCount) {
        return findBlockWithVersion(skipCount, Block.INSTANT_BLOCK_VERSION);
    }

    @Override
    public Block findLastInstantBlock() {
        return findInstantBlock(0);
    }

    @Override
    public Block findRegularBlock(int skipCount) {
        return findBlockWithVersion(skipCount, Block.REGULAR_BLOCK_VERSION);
    }

    @Override
    public Block findRegularBlock() {
        return findRegularBlock(0);
    }


    @Override
    public Block findLastBlock(int timestamp) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp <= ? ORDER BY timestamp DESC LIMIT 1")) {
            pstmt.setInt(1, timestamp);
            Block block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Set<Long> getBlockGenerators(int startHeight) {
        Set<Long> generators = new HashSet<>();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                        "SELECT generator_id, COUNT(generator_id) AS count FROM block WHERE height >= ? GROUP BY generator_id")) {
            pstmt.setInt(1, startHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("count") > 1) {
                        generators.add(rs.getLong("generator_id"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return generators;
    }

    @Override
    public Block loadBlock(Connection con, ResultSet rs) {
        return loadBlock(con, rs, false);
    }

    @Override
    public Block loadBlock(Connection con, ResultSet rs, boolean loadTransactions) {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            long previousBlockId = rs.getLong("previous_block_id");
            long totalAmountATM = rs.getLong("total_amount");
            long totalFeeATM = rs.getLong("total_fee");
            int payloadLength = rs.getInt("payload_length");
            long generatorId = rs.getLong("generator_id");
            byte[] previousBlockHash = rs.getBytes("previous_block_hash");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long baseTarget = rs.getLong("base_target");
            long nextBlockId = rs.getLong("next_block_id");
            if (nextBlockId == 0 && !rs.wasNull()) {
                throw new IllegalStateException("Attempting to load invalid block");
            }
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");
            long id = rs.getLong("id");
            int timeout = rs.getInt("timeout");
            return new BlockImpl(version, timestamp, previousBlockId, totalAmountATM, totalFeeATM, payloadLength, payloadHash,
                    generatorId, generationSignature, blockSignature, previousBlockHash,
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, timeout, loadTransactions ?
                    lookupTransactionDao().findBlockTransactions(con,
                    id) :
                    null);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void saveBlock(Connection con, Block block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, "
                    + "total_amount, total_fee, payload_length, previous_block_hash, next_block_id, cumulative_difficulty, "
                    + "base_target, height, generation_signature, block_signature, payload_hash, generator_id, timeout) "
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, block.getId());
                pstmt.setInt(++i, block.getVersion());
                pstmt.setInt(++i, block.getTimestamp());
                DbUtils.setLongZeroToNull(pstmt, ++i, block.getPreviousBlockId());
                pstmt.setLong(++i, block.getTotalAmountATM());
                pstmt.setLong(++i, block.getTotalFeeATM());
                pstmt.setInt(++i, block.getPayloadLength());
                pstmt.setBytes(++i, block.getPreviousBlockHash());
                pstmt.setLong(++i, 0L); // next_block_id set to 0 at first
                pstmt.setBytes(++i, block.getCumulativeDifficulty().toByteArray());
                pstmt.setLong(++i, block.getBaseTarget());
                pstmt.setInt(++i, block.getHeight());
                pstmt.setBytes(++i, block.getGenerationSignature());
                pstmt.setBytes(++i, block.getBlockSignature());
                pstmt.setBytes(++i, block.getPayloadHash());
                pstmt.setLong(++i, block.getGeneratorId());
                pstmt.setInt(++i, block.getTimeout());
                pstmt.executeUpdate();
                lookupTransactionDao().saveTransactions(con, block.getTransactions());
            }
            if (block.getPreviousBlockId() != 0) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
                    pstmt.executeUpdate();
                }
                Block previousBlock;
                synchronized (blockCache) {
                    previousBlock = blockCache.get(block.getPreviousBlockId());
                }
                if (previousBlock != null) {
                    previousBlock.setNextBlockId(block.getId());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    //set next_block_id to null instead of 0 to indicate successful block push
    @Override
    public void commit(Block block) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")) {
            pstmt.setLong(1, block.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void deleteBlocksFromHeight(int height) {
        long blockId;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                blockId = rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        LOG.debug("Deleting blocks starting from height %s", height);
        deleteBlocksFrom(blockId);
    }

    // relying on cascade triggers in the database to delete the transactions and public keys for all deleted blocks
    @Override
    public Block deleteBlocksFrom(long blockId) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        if (!inTransaction) {
            Block lastBlock;
            try {
                dataSource.begin();
                // TODO: Recursion, check if safe...
                lastBlock = deleteBlocksFrom(blockId);
                dataSource.commit();
            } catch (Exception e) {
                dataSource.rollback();
                throw e;
            }
            return lastBlock;
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT db_id FROM block WHERE timestamp >= "
                     + "IFNULL ((SELECT timestamp FROM block WHERE id = ?), " + Integer.MAX_VALUE + ") ORDER BY timestamp DESC");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?")) {
            try {
                pstmtSelect.setLong(1, blockId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    dataSource.commit(false);
                    while (rs.next()) {
        	            pstmtDelete.setLong(1, rs.getLong("db_id"));
            	        pstmtDelete.executeUpdate();
                        dataSource.commit(false);
                    }
	            }
                Block lastBlock = findLastBlock();
                lastBlock.setNextBlockId(0);
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")) {
                    pstmt.setLong(1, lastBlock.getId());
                    pstmt.executeUpdate();
                }
                // do not end existing transaction
                dataSource.commit(false);
                return lastBlock;
            } catch (SQLException e) {
                dataSource.rollback(false);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

    @Override
    public void deleteAll() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            try {
                dataSource.begin();
                deleteAll();
                dataSource.commit();
            } catch (Exception e) {
                dataSource.rollback();
                throw e;
            }
            return;
        }
        LOG.info("Deleting blockchain...");
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                tablesRegistry.getDerivedTables().forEach(table -> {
                    try {
                        stmt.executeUpdate("TRUNCATE TABLE " + table.toString());
                    } catch (SQLException ignore) {}
                });
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                dataSource.commit(false);
            } catch (SQLException e) {
                dataSource.rollback(false);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

}
