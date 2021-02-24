/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.blockchain;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Set;

public interface BlockDao {

    BlockEntity findBlock(long blockId, TransactionalDataSource dataSource);

    boolean hasBlock(long blockId);

    BlockEntity findFirstBlock();

    boolean hasBlock(long blockId, int height, TransactionalDataSource dataSource);

    long findBlockIdAtHeight(int height, TransactionalDataSource dataSource);

    BlockEntity findBlockAtHeight(int height, TransactionalDataSource dataSource);

    BlockEntity findLastBlock();

    List<BlockEntity> getBlocksByAccount(TransactionalDataSource dataSource, long accountId, int from, int to, int timestamp);

    List<BlockEntity> getBlocks(TransactionalDataSource dataSource, int from, int to, int timestamp);

    long getBlockCount(TransactionalDataSource dataSource, int from, int to);

    int getBlockCount(TransactionalDataSource dataSource, long accountId);

    List<Long> getBlockIdsAfter(int height, int limit);

    List<BlockEntity> getBlocksAfter(int height, List<Long> blockList, List<BlockEntity> result, TransactionalDataSource dataSource, int index);

    BlockEntity findBlockWithVersion(int skipCount, int version);

    List<byte[]> getBlockSignaturesFrom(int from, int to);

    BlockEntity findLastBlock(int timestamp);

    Set<Long> getBlockGenerators(int startHeight, int limit);

    List<BlockEntity> getBlocks(Connection con, PreparedStatement pstmt);

    void saveBlock(BlockEntity block);

    //set next_block_id to null instead of 0 to indicate successful block push
    void commit(long blockId);

    void deleteBlocksFromHeight(int height);

    /**
     * Assume, that all derived tables were already rolled back
     * This method will delete blocks and transactions at height greater than or equal to height of block specified by blockId
     *
     * @param blockId id of the block, after which all blocks with transactions should be deleted inclusive
     * @return current last block in blockchain after deletion
     */
    BlockEntity deleteBlocksFrom(long blockId);

    void deleteAll();

    List<Block> getBlocksAfter(int height, int limit);
}
