package com.apollocurrency.aplwallet.apl.core.dao.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface TransactionDao {

    Transaction findTransaction(long transactionId, TransactionalDataSource dataSource);

    Transaction findTransaction(long transactionId, int height, TransactionalDataSource dataSource);

    Transaction findTransactionByFullHash(byte[] fullHash, TransactionalDataSource dataSource);

    Transaction findTransactionByFullHash(byte[] fullHash, int height, TransactionalDataSource dataSource);

    boolean hasTransaction(long transactionId, TransactionalDataSource dataSource);

    boolean hasTransaction(long transactionId, int height, TransactionalDataSource dataSource);

    boolean hasTransactionByFullHash(byte[] fullHash, TransactionalDataSource dataSource);

    boolean hasTransactionByFullHash(byte[] fullHash, int height, TransactionalDataSource dataSource);

    byte[] getFullHash(long transactionId, TransactionalDataSource dataSource);

    Transaction loadTransaction(Connection con, ResultSet rs) throws AplException.NotValidException;

    List<Transaction> findBlockTransactions(long blockId, TransactionalDataSource dataSource);

    long getBlockTransactionsCount(long blockId, TransactionalDataSource dataSource);

    List<Transaction> findBlockTransactions(Connection con, long blockId);

    List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp);

    void saveTransactions(Connection con, List<Transaction> transactions);

    int getTransactionCount();

    Long getTransactionCount(TransactionalDataSource dataSource, int from, int to);

    List<Transaction> loadTransactionList(Connection conn, PreparedStatement pstmt) throws SQLException, AplException.NotValidException;

//    DbIterator<Transaction> getAllTransactions();

    List<Transaction> getTransactions(TransactionalDataSource dataSource,
                                      long accountId, int numberOfConfirmations, byte type, byte subtype,
                                      int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                      int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                      int height, int prunableExpiration);

    int getTransactionCountByFilter(TransactionalDataSource dataSource,
                                    long accountId, int numberOfConfirmations, byte type, byte subtype,
                                    int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
                                    boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
                                    int height, int prunableExpiration);

    DbIterator<Transaction> getTransactions(byte type, byte subtype, int from, int to);

    List<Transaction> getTransactions(int fromDbId, int toDbId);

    List<TransactionDbInfo> getTransactionsBeforeHeight(int height);

    int getTransactionCount(long accountId, byte type, byte subtype);

    DbIterator<Transaction> getTransactions(Connection con, PreparedStatement pstmt);

}
