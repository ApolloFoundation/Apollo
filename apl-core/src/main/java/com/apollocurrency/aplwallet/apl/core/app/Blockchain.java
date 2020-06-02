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

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface Blockchain {

    Block getLastBlock();

    void setLastBlock(Block block);

    /**
     * Update internal state of blockchain
     * Should be called, when database was created from scratch
     */
    void update();

    Block getLastBlock(int timestamp);

    int getHeight();

    int getLastBlockTimestamp();

    Block getBlock(long blockId);

    Block getBlockAtHeight(int height);

    boolean hasBlock(long blockId);

    boolean hasBlockInShards(long blockId);

    @Deprecated
    DbIterator<Block> getBlocks(int from, int to, int timestamp);

    Stream<Block> getBlocksStream(int from, int to, int timestamp);

    Block findFirstBlock();

    @Deprecated
    DbIterator<Block> getBlocksByAccount(long accountId, int from, int to, int timestamp);

    Stream<Block> getBlocksByAccountStream(long accountId, int from, int to, int timestamp);

    Block findLastBlock();

    Block loadBlock(Connection con, ResultSet rs, boolean loadTransactions);

    void saveBlock(Connection con, Block block);

    void commit(Block block);

    Long getBlockCount(TransactionalDataSource dataSource, int from, int to);

    int getBlockCount(long accountId);


    Block getShardInitialBlock();

    void setShardInitialBlock(Block block);

    List<Long> getBlockIdsAfter(long blockId, int limit);

    List<byte[]> getBlockSignaturesFrom(int fromHeight, int toHeight);

    List<Block> getBlocksAfter(long blockId, List<Long> blockList);

    long getBlockIdAtHeight(int height);

    EcBlockData getECBlock(int timestamp);

    void deleteBlocksFromHeight(int height);

    Block deleteBlocksFrom(long blockId);

    void deleteAll();

    Transaction getTransaction(long transactionId);

    Transaction findTransaction(long transactionId, int height);

    Transaction getTransactionByFullHash(String fullHash);

    Transaction findTransactionByFullHash(byte[] fullHash);

    Transaction findTransactionByFullHash(byte[] fullHash, int height);

    boolean hasTransaction(long transactionId);

    boolean hasTransaction(long transactionId, int height);

    boolean hasTransactionByFullHash(String fullHash);

    boolean hasTransactionByFullHash(byte[] fullHash);

    boolean hasTransactionByFullHash(byte[] fullHash, int height);

    /**
     * <p>Get transaction height by using fullHash restricted it by heightLimit parameter.</p>
     * <p>This method will return height of transaction in blockchain, even if transaction currently not exist or not available</p>
     *
     * @param fullHash    fullHash of transaction to retrieved
     * @param heightLimit upper bound which should not be crossed by returned transaction height
     * @return height of transaction or null when collision for hash occurred, transaction height greater than heightLimit or transaction with such hash not found
     */
    Integer getTransactionHeight(byte[] fullHash, int heightLimit);

    byte[] getFullHash(long transactionId);

    Transaction loadTransaction(Connection con, ResultSet rs) throws AplException.NotValidException;

    int getTransactionCount();

    Long getTransactionCount(TransactionalDataSource dataSource, int from, int to);

    List<Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                      int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                      int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate);

    List<Transaction> getBlockTransactions(long blockId);

    boolean isInitialized();

    boolean hasBlock(long blockId, int height);

    int getTransactionCount(long accountId, byte type, byte subtype);

    DbIterator<Transaction> getTransactions(Connection con, PreparedStatement pstmt);

    List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp);

    DbIterator<Transaction> getTransactions(byte type, byte subtype, int from, int to);

    Set<Long> getBlockGenerators(int limit);

    List<TransactionDbInfo> getTransactionsBeforeHeight(int height);

    boolean hasConfirmations(long id, int confirmations);

    boolean isExpired(Transaction tx);
}
