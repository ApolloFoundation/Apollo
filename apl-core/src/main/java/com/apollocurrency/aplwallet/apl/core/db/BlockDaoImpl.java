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

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockNotFoundException;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class BlockDaoImpl implements BlockDao {
    private static final Logger LOG = getLogger(BlockDaoImpl.class);

//    @Inject
//    @CacheProducer
//    @CacheType(PUBLIC_KEY_CACHE_NAME)
//    private Cache<Long, BlockIndex> publicKeyCache;

    private final DatabaseManager databaseManager;

    @Inject
    public BlockDaoImpl(DatabaseManager databaseManager) {
        // this.blockCacheSize = blockCacheSize;
        this.databaseManager = Objects.requireNonNull(databaseManager, "DatabaseManager cannot be null");
    }

    private void clearBlockCache() {
//        synchronized (blockCache) {
//            blockCache.clear();
//            heightMap.clear();
//            transactionCache.clear();
//        }
    }

    @Transactional(readOnly = true)
    @Override
    public Block findBlock(long blockId, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
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
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


    @Transactional(readOnly = true)
    @Override
    public boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE, databaseManager.getDataSource());
    }

    @Override
    public Block findFirstBlock() {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block order by db_id LIMIT 1")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                Block block = null;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
                return block;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasBlock(long blockId, int height, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT height FROM block WHERE id = ? AND (next_block_id <> 0 OR next_block_id IS NULL)")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public long findBlockIdAtHeight(int height, TransactionalDataSource dataSource) {
        // Check the cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return rs.getLong("id");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Block findBlockAtHeight(int height, TransactionalDataSource dataSource) {
        // Check the cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                Block block;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                } else {
                    throw new BlockNotFoundException("Block at height " + height + " not found in database!");
                }
                return block;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
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
        }
        catch (SQLException e) {
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
        }
        catch (SQLException e) {
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
        }
        catch (SQLException e) {
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
        }
        catch (SQLException e) {
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
        }
        catch (SQLException e) {
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
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Long> getBlockIdsAfter(int height, int limit) {
        // Search the database
        List<Long> result;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection()) {
                result = new ArrayList<>(limit);
                try (PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height > ? ORDER BY height ASC LIMIT ?")) {
                    pstmt.setLong(1, height);
                    pstmt.setInt(2, limit);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            result.add(rs.getLong("id"));
                        }
                    }
                }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public List<Block> getBlocksAfter(int height, List<Long> blockIdList, List<Block> result, TransactionalDataSource dataSource, int index) {
        // Search the database
        try (Connection con = dataSource.getConnection()) {
            return getBlocksAfter(height, blockIdList, result, con, index);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Block> getBlocksAfter(int height, List<Long> blockIdList, List<Block> result, Connection con, int index) {
        // Search the database
        try (PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                     + "WHERE height > ? "
                     + "ORDER BY height ASC LIMIT ?")) {
            pstmt.setLong(1, height);
            pstmt.setInt(2, blockIdList.size() - index);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Block block = this.loadBlock(con, rs);
                    if (block.getId() != blockIdList.get(index++)) {
                        LOG.debug("Block id {} not equal to {}", block.getId(), blockIdList.get(index - 1));
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
                     "SELECT * FROM block WHERE version = ? ORDER BY timestamp DESC LIMIT 1 OFFSET ?")) {
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
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
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
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Set<Long> getBlockGenerators(int startHeight, int limit) {
        Set<Long> generators = new HashSet<>();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT generator_id, COUNT(generator_id) AS count FROM block WHERE height >= ? GROUP BY generator_id having count > 1 limit ?")) {
            pstmt.setInt(1, startHeight);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    generators.add(rs.getLong("generator_id"));
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return generators;
    }

    @Override
    public Block loadBlock(Connection con, ResultSet rs) {
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
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, timeout,
                    null);
        }
        catch (SQLException e) {
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
            }
            if (block.getPreviousBlockId() != 0) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
                    pstmt.executeUpdate();
                }
//                Update next block id for cached block
//                Block previousBlock;
//                synchronized (blockCache) {
//                    previousBlock = blockCache.get(block.getPreviousBlockId());
//                }
//                if (previousBlock != null) {
//                    previousBlock.setNextBlockId(block.getId());
//                }
            }
        }
        catch (SQLException e) {
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
        }
        catch (SQLException e) {
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
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        LOG.debug("Deleting blocks starting from height %s", height);
        deleteBlocksFrom(blockId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Block deleteBlocksFrom(long blockId) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        if (!inTransaction) {
            Block lastBlock;
            try {
                dataSource.begin();
                lastBlock = deleteBlocksFrom(blockId);
                dataSource.commit();
            }
            catch (Exception e) {
                dataSource.rollback();
                throw e;
            }
            return lastBlock;
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtBlockSelect = con.prepareStatement("SELECT db_id, id FROM block WHERE timestamp >= "
                     + "IFNULL ((SELECT timestamp FROM block WHERE id = ?), " + Integer.MAX_VALUE + ") ORDER BY timestamp DESC");
             PreparedStatement pstmtBlockDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?");
             PreparedStatement pstmtTransactionDelete = con.prepareStatement("DELETE FROM transaction WHERE db_id = ?");
             PreparedStatement pstmtTransactionSelect = con.prepareStatement("SELECT db_id FROM transaction WHERE block_id = ?")
        ) {
            try {
                pstmtBlockSelect.setLong(1, blockId);
                try (ResultSet rs = pstmtBlockSelect.executeQuery()) {
                    dataSource.commit(false);
                    while (rs.next()) {
                        pstmtBlockDelete.setLong(1, rs.getLong("db_id"));
                        pstmtBlockDelete.executeUpdate();
                        pstmtTransactionSelect.setLong(1, rs.getLong("id"));
                        try (ResultSet txRs = pstmtTransactionSelect.executeQuery()) {
                            while (txRs.next()) {
                                pstmtTransactionDelete.setLong(1, txRs.getLong("db_id"));
                                pstmtTransactionDelete.executeUpdate();
                            }
                        }
                        dataSource.commit(false);
                    }
                }
                Block lastBlock = findLastBlock();
                if (lastBlock == null) {
                    // should never happen, but possible in rare error cases
                    LOG.warn("Block was not found in 'main db' by blockId = {}", blockId);
                } else {
                    lastBlock.setNextBlockId(0);
                }
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")) {
                    pstmt.setLong(1, lastBlock.getId());
                    pstmt.executeUpdate();
                }
                // do not end existing transaction
                dataSource.commit(false);
                return lastBlock;
            }
            catch (SQLException e) {
                dataSource.rollback(false);
                throw e;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        finally {
//            clearBlockCache();
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
            }
            catch (Exception e) {
                dataSource.rollback();
                throw e;
            }
            return;
        }
        LOG.debug("Deleting blockchain...");
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                LOG.debug("DONE Deleting blockchain...");
            } catch (SQLException e) {
                dataSource.rollback(false);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
