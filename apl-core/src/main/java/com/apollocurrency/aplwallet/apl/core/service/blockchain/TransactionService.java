/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ChatInfo;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;

import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface TransactionService {
    Transaction findTransaction(long transactionId);

    Transaction findTransactionCrossSharding(long transactionId, int height);

    Transaction findTransactionByFullHash(byte[] fullHash);

    Transaction findTransactionCrossShardingByFullHash(byte[] fullHash, int height);

    List<Transaction> findBlockTransactionsCrossSharding(long blockId);

    List<PrunableTransaction> findPrunableTransactions(int minTimestamp, int maxTimestamp);

    boolean hasTransaction(long transactionId);

    boolean hasTransaction(long transactionId, int height);

    boolean hasTransactionByFullHash(byte[] fullHash);

    boolean hasTransactionByFullHash(byte[] fullHash, int height);

    byte[] getFullHash(long transactionId);

    long getBlockTransactionsCountCrossSharding(long blockId);

    void saveTransactions(List<Transaction> transactions);

    int getTransactionCount();

    Long getTransactionCount(int from, int to);

    List<Transaction> getTransactionsCrossShardingByAccount(long accountId, int currentBlockChainHeight, int numberOfConfirmations, byte type, byte subtype,
                                                            int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                                            int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate);

    List<Transaction> getTransactionsByFilter(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                              int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                              int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                              int height, int prunableExpiration);

    int getTransactionCountByFilter(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                    int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                    boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                    int height, int prunableExpiration);

    List<Transaction> getTransactionsChatHistory(long account1, long account2, int from, int to);

    List<ChatInfo> getChatAccounts(long accountId, int from, int to);

    List<Transaction> getTransactions(byte type, byte subtype, int from, int to);

    List<Transaction> getTransactions(int fromDbId, int toDbId);

    List<TransactionDbInfo> getTransactionsBeforeHeight(int height);

    int getTransactionCount(long accountId, byte type, byte subtype);

    int getTransactionsCount(List<Long> accounts, byte type, byte subtype,
                             int startTime, int endTime,
                             int fromHeight, int toHeight,
                             String sortOrder,
                             int from, int to);

    List<TxReceipt> getTransactions(List<Long> accounts, byte type, byte subtype,
                                    int startTime, int endTime,
                                    int fromHeight, int toHeight,
                                    String sortOrder,
                                    int from, int to);

}
