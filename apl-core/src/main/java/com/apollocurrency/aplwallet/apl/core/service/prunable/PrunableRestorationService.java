package com.apollocurrency.aplwallet.apl.core.service.prunable;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;

import java.util.Set;

public interface PrunableRestorationService {
    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);

    Set<Long> getPrunableTransactions();

    boolean remove(long transactionId);
}
