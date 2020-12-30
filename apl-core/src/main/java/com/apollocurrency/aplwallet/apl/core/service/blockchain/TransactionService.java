/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface TransactionService {
    Transaction findTransaction(long transactionId);

    Transaction findTransaction(long transactionId, int height);

    Transaction findTransaction(long transactionId, int height, TransactionalDataSource dataSource);

    Transaction findTransactionByFullHash(byte[] fullHash);

    Transaction findTransactionByFullHash(byte[] fullHash, int height);

    Transaction findTransactionByFullHash(byte[] fullHash, int height, TransactionalDataSource dataSource);

    boolean hasTransaction(long transactionId);

    boolean hasTransaction(long transactionId, int height);

    boolean hasTransactionByFullHash(byte[] fullHash);

    boolean hasTransactionByFullHash(byte[] fullHash, int height);

    byte[] getFullHash(long transactionId);

    List<Transaction> findBlockTransactions(long blockId, TransactionalDataSource dataSource);

    long getBlockTransactionsCount(long blockId);

    void saveTransactions(Connection con, List<Transaction> transactions);

    int getTransactionCount();

    Long getTransactionCount(int from, int to);

    List<Transaction> getTransactions(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                      int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                      int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                      int height, int prunableExpiration);

    int getTransactionCountByFilter(long accountId, int numberOfConfirmations, byte type, byte subtype,
                                    int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                    boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                    int height, int prunableExpiration);

    List<Transaction> getTransactions(Connection con, PreparedStatement pstmt);

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

    List<Transaction> getTransactions(long accountId, int currentBlockChainHeight, int numberOfConfirmations, byte type, byte subtype,
                                      int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                      int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate);
}
