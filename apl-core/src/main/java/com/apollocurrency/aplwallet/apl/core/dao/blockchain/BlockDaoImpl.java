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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.BlockNotFoundException;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
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
    static final String SELECT_ALL_AFTER_HEIGHT_QUERY = "SELECT * FROM block WHERE height > ? ORDER BY height LIMIT ?";

    private final BlockEntityRowMapper entityRowMapper;
    private final DatabaseManager databaseManager;

    @Inject
    public BlockDaoImpl(DatabaseManager databaseManager, BlockEntityRowMapper blockEntityRowMapper) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "DatabaseManager cannot be null");
        this.entityRowMapper = Objects.requireNonNull(blockEntityRowMapper);
    }

    @Transactional(readOnly = true)
    @Override
    public BlockEntity findBlock(long blockId, TransactionalDataSource dataSource) {
        // Check the block cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ?")
        ) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockEntity blockEntity = null;
                if (rs.next()) {
                    blockEntity = entityRowMapper.map(rs, null);
                }
                return blockEntity;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE, databaseManager.getDataSource());
    }

    @Override
    public BlockEntity findFirstBlock() {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block order by db_id LIMIT 1")
        ) {
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockEntity block = null;
                if (rs.next()) {
                    block = entityRowMapper.map(rs, null);
                }
                return block;
            }
        } catch (SQLException e) {
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
                 "SELECT height FROM block WHERE id = ? AND (next_block_id <> 0 OR next_block_id IS NULL)")
        ) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public long findBlockIdAtHeight(int height, TransactionalDataSource dataSource) {
        // Check the cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")
        ) {
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

    @Transactional(readOnly = true)
    @Override
    public BlockEntity findBlockAtHeight(int height, TransactionalDataSource dataSource) {
        // Check the cache
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")
        ) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockEntity block;
                if (rs.next()) {
                    block = entityRowMapper.map(rs, null);
                } else {
                    throw new BlockNotFoundException("Block at height " + height + " not found in database!");
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public BlockEntity findLastBlock() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
             PreparedStatement pstmt = con.prepareStatement(
                 "SELECT * FROM block WHERE next_block_id <> 0 OR next_block_id IS NULL ORDER BY `timestamp` DESC LIMIT 1")
        ) {
            BlockEntity block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = entityRowMapper.map(rs, null);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<byte[]> getBlockSignaturesFrom(int fromHeight, int toHeight) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        List<byte[]> blockSignatures = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT block_signature FROM block "
                 + "WHERE height >= ? AND height < ? ")
        ) {
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
    public List<BlockEntity> getBlocksByAccount(TransactionalDataSource dataSource, long accountId, int from, int to, int timestamp) {
        LOG.trace("start getBlocksByAccount(dataSource={}, accountId={}, from={}, to={}, timestamp={} )...",
            dataSource != null ? dataSource.getDbIdentity() : null, accountId, from, to, timestamp);
        if (dataSource == null) {
            dataSource = databaseManager.getDataSource();
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE generator_id = ? "
                 + (timestamp > 0 ? " AND `timestamp` >= ? " : " ") + "ORDER BY height DESC"
                 + DbUtils.limitsClause(from, to))
        ) {
            int i = 0;
            pstmt.setLong(++i, accountId);
            if (timestamp > 0) {
                pstmt.setInt(++i, timestamp);
            }
            DbUtils.setLimits(++i, pstmt, from, to);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<BlockEntity> getBlocks(TransactionalDataSource dataSource, int from, int to, int timestamp) {
        LOG.debug("start getBlocks DbIter( from={}, to={}, timestamp={} )...", from, to, timestamp);
        if (dataSource == null) {
            dataSource = databaseManager.getDataSource();
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                 "SELECT * FROM block WHERE height <= ? AND height >= ? and `timestamp` >= ? ORDER BY height DESC");
        ) {
            pstmt.setInt(1, from);
            pstmt.setInt(2, to);
            pstmt.setInt(3, timestamp);
            return getBlocks(con, pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public long getBlockCount(TransactionalDataSource dataSource, int from, int to) {
        if (dataSource == null) {
            // select from main db
            dataSource = databaseManager.getDataSource();
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT count(*) as blockCount FROM block WHERE height >= ? AND height < ?")
        ) {
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
    public int getBlockCount(TransactionalDataSource dataSource, long accountId) {
        if (dataSource == null) {
            dataSource = databaseManager.getDataSource();
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM block WHERE generator_id = ?")
        ) {
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
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return result;
    }

    @Override
    public List<BlockEntity> getBlocksAfter(int height, List<Long> blockIdList, List<BlockEntity> result, TransactionalDataSource dataSource, int index) {
        // Search the database
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                 + "WHERE height > ? "
                 + "ORDER BY height ASC LIMIT ?")
        ) {
            pstmt.setLong(1, height);
            pstmt.setInt(2, blockIdList.size() - index);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BlockEntity block = entityRowMapper.map(rs, null);
                    if (block.getId() != blockIdList.get(index++)) {
                        LOG.debug("Block id {} not equal to {}", block.getId(), blockIdList.get(index - 1));
                        break;
                    }
                    result.add(block);
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public BlockEntity findBlockWithVersion(int skipCount, int version) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                 "SELECT * FROM block WHERE version = ? ORDER BY `timestamp` DESC LIMIT 1 OFFSET ?")
        ) {
            int i = 0;
            pstmt.setInt(++i, version);
            pstmt.setInt(++i, skipCount);
            BlockEntity block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = entityRowMapper.map(rs, null);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public BlockEntity findLastBlock(int timestamp) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE `timestamp` <= ? ORDER BY `timestamp` DESC LIMIT 1")
        ) {
            pstmt.setInt(1, timestamp);
            BlockEntity block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = entityRowMapper.map(rs, null);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Set<Long> getBlockGenerators(int startHeight, int limit) {
        Set<Long> generators = new HashSet<>();
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                 "SELECT generator_id, COUNT(generator_id) AS count FROM block WHERE height >= ? GROUP BY generator_id having count > 1 limit ?")
        ) {
            pstmt.setInt(1, startHeight);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    generators.add(rs.getLong("generator_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return generators;
    }

    @Override
    public List<BlockEntity> getBlocks(Connection con, PreparedStatement pstmt) {
        List<BlockEntity> blocks = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                BlockEntity blockEntity = entityRowMapper.mapWithException(rs, null);
                blocks.add(blockEntity);
            }
        } catch (SQLException | AplException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return blocks;
    }

    @Override
    public void saveBlock(BlockEntity block) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try {
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, `timestamp`, previous_block_id, "
                     + "total_amount, total_fee, payload_length, previous_block_hash, next_block_id, cumulative_difficulty, "
                     + "base_target, height, generation_signature, block_signature, payload_hash, generator_id, timeout) "
                     + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            ) {
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
                try (Connection con = dataSource.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")
                ) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    //set next_block_id to null instead of 0 to indicate successful block push
    @Override
    public void commit(long blockId) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")
        ) {
            pstmt.setLong(1, blockId);
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
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")
        ) {
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
        LOG.debug("Deleting blocks starting from height {}", height);
        deleteBlocksFrom(blockId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockEntity deleteBlocksFrom(long blockId) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        if (!inTransaction) {
            BlockEntity lastBlock;
            try {
                dataSource.begin();
                lastBlock = deleteBlocksFrom(blockId);
                dataSource.commit();
            } catch (Exception e) {
                dataSource.rollback();
                throw e;
            }
            return lastBlock;
        }
        try (Connection con = dataSource.getConnection();
             @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
             @DatabaseSpecificDml(DmlMarker.IFNULL_USE)
             PreparedStatement pstmtBlockSelect = con.prepareStatement("SELECT db_id, id FROM block WHERE `timestamp` >= "
                 + "IFNULL ((SELECT `timestamp` FROM block WHERE id = ?), " + Integer.MAX_VALUE + ") ORDER BY `timestamp` DESC");
             PreparedStatement pstmtBlockDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?");
//             PreparedStatement pstmtTransactionDelete = con.prepareStatement("DELETE TX, US from transaction AS TX LEFT JOIN update_status AS US ON TX.id = US.transaction_id WHERE TX.db_id = ?");
             PreparedStatement pstmtTransactionDelete = con.prepareStatement("DELETE FROM transaction WHERE db_id = ?");
             PreparedStatement pstmtTransactionSelect = con.prepareStatement("SELECT db_id FROM transaction WHERE block_id = ?");
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
                BlockEntity lastBlock = findLastBlock();
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
            } catch (SQLException e) {
                dataSource.rollback(false);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
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
        LOG.debug("Deleting blockchain...");
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()
        ) {
            stmt.executeUpdate("TRUNCATE TABLE transaction");
            stmt.executeUpdate("TRUNCATE TABLE block");
            LOG.debug("DONE Deleting blockchain...");
        } catch (SQLException e) {
            dataSource.rollback(false);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<BlockEntity> getBlocksAfter(int height, int limit) {
        List<BlockEntity> resultList = new ArrayList<>();
        try (Connection con = databaseManager.getDataSource().getConnection();
            PreparedStatement pstmt = con.prepareStatement(SELECT_ALL_AFTER_HEIGHT_QUERY)) {
            pstmt.setLong(1, height);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BlockEntity block = entityRowMapper.map(rs, null);
                    resultList.add(block);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return resultList;
    }
}
