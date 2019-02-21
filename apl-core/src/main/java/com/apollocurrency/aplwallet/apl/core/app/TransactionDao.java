package com.apollocurrency.aplwallet.apl.core.app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.AplException;

public interface TransactionDao {

    Transaction findTransaction(long transactionId);

    Transaction findTransaction(long transactionId, int height);

    Transaction findTransactionByFullHash(byte[] fullHash);

    Transaction findTransactionByFullHash(byte[] fullHash, int height);

    boolean hasTransaction(long transactionId);

    boolean hasTransaction(long transactionId, int height);

    boolean hasTransactionByFullHash(byte[] fullHash);

    boolean hasTransactionByFullHash(byte[] fullHash, int height);

    byte[] getFullHash(long transactionId);

    Transaction loadTransaction(Connection con, ResultSet rs) throws AplException.NotValidException;

    List<Transaction> findBlockTransactions(long blockId);

    List<Transaction> findBlockTransactions(Connection con, long blockId);

    List<PrunableTransaction> findPrunableTransactions(Connection con, int minTimestamp, int maxTimestamp);

    void saveTransactions(Connection con, List<Transaction> transactions);

    int getTransactionCount();

    DbIterator<Transaction> getAllTransactions();

    DbIterator<Transaction> getTransactions(
            long accountId, int numberOfConfirmations, byte type, byte subtype,
            int blockTimestamp, boolean withMessage, boolean phasedOnly, boolean nonPhasedOnly,
            int from, int to, boolean includeExpiredPrunable, boolean executedOnly, boolean includePrivate,
            int height, int prunableExpiration);

    DbIterator<Transaction> getTransactions(byte type, byte subtype, int from, int to);

    int getTransactionCount(long accountId, byte type, byte subtype);

    DbIterator<Transaction> getTransactions(Connection con, PreparedStatement pstmt);

    DbIterator<Transaction> getReferencingTransactions(long transactionId, int from, int to);

}
