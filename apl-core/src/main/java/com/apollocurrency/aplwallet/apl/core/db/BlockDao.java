/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;

public interface BlockDao {

    Block findBlock(long blockId);

    boolean hasBlock(long blockId);

    boolean hasBlock(long blockId, int height);

    long findBlockIdAtHeight(int height);

    Map<Long, Block> getBlockCache();

    SortedMap<Integer, Block> getHeightMap();

    Block findBlockAtHeight(int height);

    int getBlockCacheSize();

    Map<Long, Transaction> getTransactionCache();

    Block findLastBlock();

//    DbIterator<Block> getAllBlocks();

    DbIterator<Block> getBlocks(Connection con, PreparedStatement pstmt);

    DbIterator<Block> getBlocks(long accountId, int timestamp, int from, int to);

    DbIterator<Block> getBlocks(int from, int to);

    int getBlockCount(long accountId);

    List<Long> getBlockIdsAfter(long blockId, int limit, List<Long> result);

    List<Block> getBlocksAfter(long blockId, int limit, List<Block> result);

    List<Block> getBlocksAfter(long blockId, List<Long> blockList, List<Block> result);

    Block findBlockWithVersion(int skipCount, int version);

    Block findAdaptiveBlock(int skipCount);

    Block findLastAdaptiveBlock();

    Block findInstantBlock(int skipCount);

    Block findLastInstantBlock();

    Block findRegularBlock(int skipCount);

    List<byte[]> getBlockSignaturesFrom(int from, int to);

    Block findRegularBlock();

    Block findLastBlock(int timestamp);

    Set<Long> getBlockGenerators(int startHeight);

    Block loadBlock(Connection con, ResultSet rs);

    Block loadBlock(Connection con, ResultSet rs, boolean loadTransactions);

    void saveBlock(Connection con, Block block);

    //set next_block_id to null instead of 0 to indicate successful block push
    void commit(Block block);

    void deleteBlocksFromHeight(int height);

    // relying on cascade triggers in the database to delete the transactions and public keys for all deleted blocks
    Block deleteBlocksFrom(long blockId);

    void deleteAll();
}
