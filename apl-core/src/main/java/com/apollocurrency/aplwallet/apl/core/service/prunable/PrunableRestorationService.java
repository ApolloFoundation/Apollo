package com.apollocurrency.aplwallet.apl.core.service.prunable;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;

import java.util.Set;

public interface PrunableRestorationService {
    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);

    Set<Long> getPrunableTransactions();

    boolean remove(long transactionId);
}
