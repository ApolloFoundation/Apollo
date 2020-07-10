package com.apollocurrency.aplwallet.apl.core.service.prunable;

import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;

public interface PrunableRestorationService {
    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);

    Set<Long> getPrunableTransactions();

    boolean remove(long transactionId);
}
