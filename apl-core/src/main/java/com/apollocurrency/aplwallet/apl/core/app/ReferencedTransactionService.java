/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReferencedTransactionService {
    private static final int DEFAULT_MAX_REFERENCED_TRANSACTIONS = 9;
    private ReferencedTransactionDao referencedTransactionDao;
    private TransactionIndexDao transactionIndexDao;
    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;
    private int maxReferencedTransactions;


    public ReferencedTransactionService(
            ReferencedTransactionDao referencedTransactionDao, TransactionIndexDao transactionIndexDao,
            Blockchain blockchain, BlockchainConfig blockchainConfig, int maxReferencedTransactions) {
        this.referencedTransactionDao = referencedTransactionDao;
        this.transactionIndexDao = transactionIndexDao;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.maxReferencedTransactions = maxReferencedTransactions;
    }

    @Inject
    public ReferencedTransactionService(
            ReferencedTransactionDao referencedTransactionDao, TransactionIndexDao transactionIndexDao,
            Blockchain blockchain, BlockchainConfig blockchainConfig) {
        this(referencedTransactionDao, transactionIndexDao, blockchain, blockchainConfig, DEFAULT_MAX_REFERENCED_TRANSACTIONS);
    }

    public boolean hasAllReferencedTransactions(Transaction transaction, int height) {

        int count = 0;
        byte[] hash = hashToBytes(transaction.getReferencedTransactionFullHash());
        while (hash != null ) {
            Integer referencedTransactionHeight = blockchain.getTransactionHeight(hash, height);
            if (referencedTransactionHeight == null
                    || referencedTransactionHeight >= height
                    || ++count > maxReferencedTransactions
                    || height - referencedTransactionHeight > blockchainConfig.getCurrentConfig().getReferencedTransactionHeightSpan()) {
                return false;
            }
            hash = getReferencedFullHash(Convert.fullHashToId(hash));
        }
        return true;
    }

    public List<Transaction> getReferencingTransactions(long transactionId, int from, Integer limit) {
        return referencedTransactionDao.getReferencingTransactions(transactionId, from, limit);
    }


    private byte[] getReferencedFullHash(long traansactionId) {
        byte[] hash = null;
        Long referencedTransactionId = referencedTransactionDao.getReferencedTransactionIdFor(traansactionId);
        if (referencedTransactionId != null) {
            hash = blockchain.getFullHash(referencedTransactionId);
        }
        return hash;
    }


    private byte[] hashToBytes(String hash) {
        if (hash == null) {
            return null;
        }
        return Convert.parseHexString(hash);
    }

}
