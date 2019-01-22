/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

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

    Block findBlockWithVersion(int skipCount, int version);

    Block findAdaptiveBlock(int skipCount);

    Block findLastAdaptiveBlock();

    Block findInstantBlock(int skipCount);

    Block findLastInstantBlock();

    Block findRegularBlock(int skipCount);

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
