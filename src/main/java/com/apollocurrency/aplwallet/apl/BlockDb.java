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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.apollocurrency.aplwallet.apl.db.DbUtils;
import com.apollocurrency.aplwallet.apl.updater.ConnectionProvider;
import org.slf4j.Logger;
public final class BlockDb {
    private static final Logger LOG = getLogger(BlockDb.class);

    /** Block cache */
    private static final int DEFAULT_BLOCK_CACHE_SIZE = 10;
    private int blockCacheSize;
    private final Map<Long, BlockImpl> blockCache;             
    private final SortedMap<Integer, BlockImpl> heightMap;     
    private final Map<Long, TransactionImpl> transactionCache;
    private final ConnectionProvider connectionProvider;
    public BlockDb(int blockCacheSize, Map<Long, BlockImpl> blockCache, SortedMap<Integer, BlockImpl> heightMap,
                   Map<Long, TransactionImpl> transactionCache, ConnectionProvider connectionProvider) {
        this.blockCacheSize = blockCacheSize;
        this.blockCache = blockCache == null ? new HashMap<>() : blockCache;
        this.heightMap = heightMap == null ? new TreeMap<>() : heightMap;
        this.transactionCache = transactionCache == null ? new HashMap<>() : transactionCache;
        if (connectionProvider == null) {
            throw new IllegalArgumentException("Connection provider is null");
        }
        this.connectionProvider = connectionProvider;
    }

    public BlockDb(ConnectionProvider connectionProvider) {
        this(DEFAULT_BLOCK_CACHE_SIZE, null, null, null, connectionProvider);
    }

    public void attachCacheListener() {
        Apl.getBlockchainProcessor().addListener((block) -> {
            synchronized (blockCache) {
                int height = block.getHeight();
                Iterator<BlockImpl> it = blockCache.values().iterator();
                while (it.hasNext()) {
                    Block cacheBlock = it.next();
                    int cacheHeight = cacheBlock.getHeight();
                    if (cacheHeight <= height - blockCacheSize || cacheHeight >= height) {
                        cacheBlock.getTransactions().forEach((tx) -> transactionCache.remove(tx.getId()));
                        heightMap.remove(cacheHeight);
                        it.remove();
                    }
                }
                block.getTransactions().forEach((tx) -> transactionCache.put(tx.getId(), (TransactionImpl)tx));
                heightMap.put(height, (BlockImpl)block);
                blockCache.put(block.getId(), (BlockImpl)block);
            }
        }, BlockchainProcessor.Event.BLOCK_PUSHED);
    }
    

     private void clearBlockCache() {
        synchronized (blockCache) {
            blockCache.clear();
            heightMap.clear();
            transactionCache.clear();
        }
    }

    public BlockImpl findBlock(long blockId) {
        // Check the block cache
        synchronized (blockCache) {
            BlockImpl block = blockCache.get(blockId);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block = null;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE);
    }

    boolean hasBlock(long blockId, int height) {
        // Check the block cache
        synchronized(blockCache) {
            BlockImpl block = blockCache.get(blockId);
            if (block != null) {
                return block.getHeight() <= height;
            }
        }
        // Search the database
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT height FROM block WHERE id = ? AND (next_block_id <> 0 OR next_block_id IS NULL)")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    long findBlockIdAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            BlockImpl block = heightMap.get(height);
            if (block != null) {
                return block.getId();
            }
        }
        // Search the database
        try (Connection con = connectionProvider.getConnection();
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

    public Map<Long, BlockImpl> getBlockCache() {
        return blockCache;
    }

    public SortedMap<Integer, BlockImpl> getHeightMap() {
        return heightMap;
    }

    BlockImpl findBlockAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            BlockImpl block = heightMap.get(height);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block;
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

    public int getBlockCacheSize() {
        return blockCacheSize;
    }

    public Map<Long, TransactionImpl> getTransactionCache() {
        return transactionCache;
    }

    public BlockImpl findLastBlock() {
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE next_block_id <> 0 OR next_block_id IS NULL ORDER BY timestamp DESC LIMIT 1")) {
            BlockImpl block = null;
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

    BlockImpl findBlockWithVersion(int skipCount, int version) {
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
             "SELECT * FROM block WHERE version = ? ORDER BY block_timestamp DESC LIMIT 1 OFFSET ?)")) {
            int i = 0;
            pstmt.setInt(++i, version);
            pstmt.setInt(++i, skipCount);
            BlockImpl block = null;
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

    BlockImpl findAdaptiveBlock(int skipCount) {
        return findBlockWithVersion(skipCount, Block.ADAPTIVE_BLOCK_VERSION);
    }

    BlockImpl findLastAdaptiveBlock() {
        return findAdaptiveBlock(0);
    }
    BlockImpl findInstantBlock(int skipCount) {
        return findBlockWithVersion(skipCount, Block.INSTANT_BLOCK_VERSION);
    }

    BlockImpl findLastInstantBlock() {
        return findInstantBlock(0);
    }
    BlockImpl findRegularBlock(int skipCount) {
        return findBlockWithVersion(skipCount, Block.REGULAR_BLOCK_VERSION);
    }

    BlockImpl findRegularBlock() {
        return findRegularBlock(0);
    }


    BlockImpl findLastBlock(int timestamp) {
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp <= ? ORDER BY timestamp DESC LIMIT 1")) {
            pstmt.setInt(1, timestamp);
            BlockImpl block = null;
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

    Set<Long> getBlockGenerators(int startHeight) {
        Set<Long> generators = new HashSet<>();
        try (Connection con = connectionProvider.getConnection();
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

    static BlockImpl loadBlock(Connection con, ResultSet rs) {
        return loadBlock(con, rs, false);
    }

    static BlockImpl loadBlock(Connection con, ResultSet rs, boolean loadTransactions) {
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
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, timeout, loadTransactions ? TransactionDb.findBlockTransactions(con,
                    id) :
                    null);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    void saveBlock(Connection con, BlockImpl block) {
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
                TransactionDb.saveTransactions(con, block.getTransactions());
            }
            if (block.getPreviousBlockId() != 0) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
                    pstmt.executeUpdate();
                }
                BlockImpl previousBlock;
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
    void commit(Block block) {
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")) {
            pstmt.setLong(1, block.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    void deleteBlocksFromHeight(int height) {
        long blockId;
        try (Connection con = connectionProvider.getConnection();
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
    BlockImpl deleteBlocksFrom(long blockId) {
        if (!connectionProvider.isInTransaction(null)) {
            BlockImpl lastBlock;
            try {
                Connection connection = connectionProvider.beginTransaction();
                // TODO: Recursion, check if safe...
                lastBlock = deleteBlocksFrom(blockId);
                connectionProvider.commitTransaction(connection);
            } catch (Exception e) {
                connectionProvider.rollbackTransaction(null);
                throw e;
            } finally {
                connectionProvider.endTransaction(null);
            }
            return lastBlock;
        }
        try (Connection con = connectionProvider.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT db_id FROM block WHERE timestamp >= "
                     + "IFNULL ((SELECT timestamp FROM block WHERE id = ?), " + Integer.MAX_VALUE + ") ORDER BY timestamp DESC");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?")) {
            try {
                pstmtSelect.setLong(1, blockId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    connectionProvider.commitTransaction(con);
                    while (rs.next()) {
        	            pstmtDelete.setLong(1, rs.getLong("db_id"));
            	        pstmtDelete.executeUpdate();
                        connectionProvider.commitTransaction(con);
                    }
	            }
                BlockImpl lastBlock = findLastBlock();
                lastBlock.setNextBlockId(0);
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")) {
                    pstmt.setLong(1, lastBlock.getId());
                    pstmt.executeUpdate();
                }
                connectionProvider.commitTransaction(con);
                return lastBlock;
            } catch (SQLException e) {
                connectionProvider.rollbackTransaction(con);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

    void deleteAll() {
        if (!connectionProvider.isInTransaction(null)) {
            try {
                Connection connection = connectionProvider.beginTransaction();
                deleteAll();
                connectionProvider.commitTransaction(connection);
            } catch (Exception e) {
                connectionProvider.rollbackTransaction(null);
                throw e;
            } finally {
                connectionProvider.endTransaction(null);
            }
            return;
        }
        LOG.info("Deleting blockchain...");
        try (Connection con = connectionProvider.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                BlockchainProcessorImpl.getInstance().getDerivedTables().forEach(table -> {
                    try {
                        stmt.executeUpdate("TRUNCATE TABLE " + table.toString());
                    } catch (SQLException ignore) {}
                });
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                connectionProvider.commitTransaction(con);
            } catch (SQLException e) {
                connectionProvider.rollbackTransaction(con);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

}
