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

import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Filter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Blockchain {

    void readLock();

    void readUnlock();

    void updateLock();

    void updateUnlock();

    void writeLock();

    void writeUnlock();

    Block getLastBlock();

    void setLastBlock(Block block);

    Block getLastBlock(int timestamp);

    int getHeight();

    int getLastBlockTimestamp();

    Block getBlock(long blockId);

    Block getBlockAtHeight(int height);

    boolean hasBlock(long blockId);

    DbIterator<? extends Block> getAllBlocks();

    DbIterator<Block> getBlocks(int from, int to);

    DbIterator<Block> getBlocks(long accountId, int timestamp);

    DbIterator<Block> getBlocks(long accountId, int timestamp, int from, int to);

    Block findLastBlock();

    Block loadBlock(Connection con, ResultSet rs, boolean loadTransactions);

    void saveBlock(Connection con, Block block);

    void commit(Block block);

    int getBlockCount(long accountId);

    DbIterator<Block> getBlocks(Connection con, PreparedStatement pstmt);

    List<Long> getBlockIdsAfter(long blockId, int limit);

    List<Block> getBlocksAfter(long blockId, int limit);

    List<Block> getBlocksAfter(long blockId, List<Long> blockList);

    long getBlockIdAtHeight(int height);

    Block getECBlock(int timestamp);

    void deleteBlocksFromHeight(int height);

    Block deleteBlocksFrom(long blockId);

    void deleteAll();

    Map<Long, Transaction> getTransactionCache();

    Transaction getTransaction(long transactionId);

    Transaction findTransaction(long transactionId, int height);

    Transaction getTransactionByFullHash(String fullHash);

    Transaction findTransactionByFullHash(byte[] fullHash);

    Transaction findTransactionByFullHash(byte[] fullHash, int height);

    boolean hasTransaction(long transactionId);

    boolean hasTransaction(long transactionId, int height);

    boolean hasTransactionByFullHash(String fullHash);

    boolean hasTransactionByFullHash(byte[] fullHash, int height);

    byte[] getFullHash(long transactionId);

    Transaction loadTransaction(Connection con, ResultSet rs) throws AplException.NotValidException;

    int getTransactionCount();

    DbIterator<Transaction> getAllTransactions();

    DbIterator<Transaction> getTransactions(long accountId, byte type, byte subtype, int blockTimestamp,
                                                      boolean includeExpiredPrunable);

    DbIterator<Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                                      int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                      int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate);

    int getTransactionCount(long accountId, byte type, byte subtype);

    DbIterator<Transaction> getTransactions(Connection con, PreparedStatement pstmt);

    List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp);

    List<Transaction> getExpectedTransactions(Filter<Transaction> filter);

    DbIterator<Transaction> getTransactions(byte type, byte subtype, int from, int to);

    DbIterator<Transaction> getReferencingTransactions(long transactionId, int from, int to);

    Set<Long> getBlockGenerators(int startHeight);

}
