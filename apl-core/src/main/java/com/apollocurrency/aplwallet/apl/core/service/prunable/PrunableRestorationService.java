package com.apollocurrency.aplwallet.apl.core.service.prunable;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;

public interface PrunableRestorationService {
    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);
}
