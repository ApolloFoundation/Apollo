/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.util.Objects;
import javax.inject.Inject;

public class ReferencedTransactionService {
    private static final int DEFAULT_MAX_REFERENCED_TRANSACTIONS = 9;
    private ReferencedTransactionDao referencedTransactionDao;
    private TransactionIndexDao transactionIndexDao;
    private Blockchain blockchain;
    private BlockchainConfig blockchainConfig;
    private int maxReferencedTransactions;


    public ReferencedTransactionService(ReferencedTransactionDao referencedTransactionDao, TransactionIndexDao transactionIndexDao, Blockchain blockchain, BlockchainConfig blockchainConfig, int maxReferencedTransactions) {
        this.referencedTransactionDao = referencedTransactionDao;
        this.transactionIndexDao = transactionIndexDao;
        this.blockchain = blockchain;
        this.blockchainConfig = blockchainConfig;
        this.maxReferencedTransactions = maxReferencedTransactions;
    }

    @Inject
    public ReferencedTransactionService(ReferencedTransactionDao referencedTransactionDao, TransactionIndexDao transactionIndexDao, Blockchain blockchain, BlockchainConfig blockchainConfig) {
        this(referencedTransactionDao, transactionIndexDao, blockchain, blockchainConfig, DEFAULT_MAX_REFERENCED_TRANSACTIONS);
    }

    public boolean hasAllReferencedTransactions(Transaction transaction, int height) {
        Long referencedTransactionId = getIdFromHash(transaction.getReferencedTransactionFullHash());
        int count = 0;
        while (referencedTransactionId != null ) {
            Transaction referencedTransaction = blockchain.getTransaction(referencedTransactionId);
            Integer referencedTransactionHeight = getHeight(referencedTransaction, referencedTransactionId);
            if (referencedTransactionHeight == null
                    || referencedTransactionHeight >= height
                    || ++count > maxReferencedTransactions
                    || height - referencedTransactionHeight > blockchainConfig.getCurrentConfig().getReferencedTransactionHeightSpan()) {
                return false;
            }
            referencedTransactionId = getId(referencedTransaction, referencedTransactionId);
        }
        return true;
    }

    private Integer getHeight(Transaction transaction, Long referencedTransactionId) {
        Objects.requireNonNull(referencedTransactionId, "referenced transaction id cannot be null here");
        Integer height;
        if (transaction == null) {
            height =  transactionIndexDao.getTransactionHeightByTransactionId(referencedTransactionId);
        } else {
            height = transaction.getHeight();
        }
        return height;
    }

    private Long getId(Transaction transaction, Long transactionId) {
        Objects.requireNonNull(transactionId, "transaction id cannot be null here");
        Long id;
        if (transaction == null) {
            id = referencedTransactionDao.getReferencedTransactionIdFor(transactionId);
        } else {
            id = getIdFromHash(transaction.getReferencedTransactionFullHash());
        }
        return id;
    }

    private Long getIdFromHash(String fullHash) {
        Long id = null;
        if (fullHash != null) {
            id = Convert.fullHashToId(Convert.parseHexString(fullHash));
        }
        return id;
    }

}
