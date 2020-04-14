/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.app.Block;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

public interface BlockDao {

    Block findBlock(long blockId, TransactionalDataSource dataSource);

    boolean hasBlock(long blockId);

    Block findFirstBlock();

    boolean hasBlock(long blockId, int height, TransactionalDataSource dataSource);

    long findBlockIdAtHeight(int height, TransactionalDataSource dataSource);

//    Map<Long, Block> getBlockCache();

//    SortedMap<Integer, Block> getHeightMap();

    Block findBlockAtHeight(int height, TransactionalDataSource dataSource);

//    int getBlockCacheSize();

//    Map<Long, Transaction> getTransactionCache();

    Block findLastBlock();

//    DbIterator<Block> getAllBlocks();

    DbIterator<Block> getBlocks(Connection con, PreparedStatement pstmt);

    @Deprecated
    DbIterator<Block> getBlocks(long accountId, int timestamp, int from, int to);

    DbIterator<Block> getBlocks(TransactionalDataSource dataSource, long accountId, int timestamp, int from, int to);

    DbIterator<Block> getBlocks(int from, int to);

    @Deprecated
    Long getBlockCount(int from, int to);

    Long getBlockCount(TransactionalDataSource dataSource, int from, int to);

    @Deprecated
    int getBlockCount(long accountId);

    int getBlockCount(TransactionalDataSource dataSource, long accountId);

    List<Long> getBlockIdsAfter(int height, int limit);

//    List<Block> getBlocksAfter(long blockId, int limit, List<Block> result);

    List<Block> getBlocksAfter(int height, List<Long> blockList, List<Block> result, TransactionalDataSource dataSource, int index);

    List<Block> getBlocksAfter(int height, List<Long> blockList, List<Block> result, Connection connection, int index);

    Block findBlockWithVersion(int skipCount, int version);


    List<byte[]> getBlockSignaturesFrom(int from, int to);

    Block findLastBlock(int timestamp);

    Set<Long> getBlockGenerators(int startHeight, int limit);

    Block loadBlock(Connection con, ResultSet rs);

    void saveBlock(Connection con, Block block);

    //set next_block_id to null instead of 0 to indicate successful block push
    void commit(Block block);

    void deleteBlocksFromHeight(int height);

    /**
     * Assume, that all derived tables were already rolled back
     * This method will delete blocks and transactions at height greater than or equal to height of block specified by blockId
     *
     * @param blockId id of the block, after which all blocks with transactions should be deleted inclusive
     * @return current last block in blockchain after deletion
     */
    Block deleteBlocksFrom(long blockId);

    void deleteAll();
}
