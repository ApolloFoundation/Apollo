package com.apollocurrency.aplwallet.apl.core.dao.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface TransactionDao {

    TransactionEntity findTransaction(long transactionId, TransactionalDataSource dataSource);

    TransactionEntity findTransaction(long transactionId, int height, TransactionalDataSource dataSource);

    TransactionEntity findTransactionByFullHash(byte[] fullHash, TransactionalDataSource dataSource);

    TransactionEntity findTransactionByFullHash(byte[] fullHash, int height, TransactionalDataSource dataSource);

    boolean hasTransaction(long transactionId, TransactionalDataSource dataSource);

    boolean hasTransaction(long transactionId, int height, TransactionalDataSource dataSource);

    boolean hasTransactionByFullHash(byte[] fullHash, TransactionalDataSource dataSource);

    boolean hasTransactionByFullHash(byte[] fullHash, int height, TransactionalDataSource dataSource);

    byte[] getFullHash(long transactionId, TransactionalDataSource dataSource);

    List<TransactionEntity> findBlockTransactions(long blockId, TransactionalDataSource dataSource);

    long getBlockTransactionsCount(long blockId, TransactionalDataSource dataSource);

    List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp);

    void saveTransactions(List<TransactionEntity> transactions);

    int getTransactionCount();

    Long getTransactionCount(TransactionalDataSource dataSource, int from, int to);

    List<TransactionEntity> getTransactions(TransactionalDataSource dataSource,
                                            long accountId, int numberOfConfirmations, byte type, byte subtype,
                                            int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                            int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                            int height, int prunableExpiration);

    int getTransactionCountByFilter(TransactionalDataSource dataSource,
                                    long accountId, int numberOfConfirmations, byte type, byte subtype,
                                    int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                    boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                    int height, int prunableExpiration);

    List<TransactionEntity> getTransactions(byte type, byte subtype, int from, int to);

    List<TransactionEntity> getTransactionsChatHistory(long account1, long account2, int from, int to);

    List<TransactionEntity> getTransactions(int fromDbId, int toDbId);

    List<TransactionDbInfo> getTransactionsBeforeHeight(int height);

    int getTransactionCount(long accountId, byte type, byte subtype);

    List<TransactionEntity> getTransactions(Connection con, PreparedStatement pstmt);

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
