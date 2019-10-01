package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;

public interface MandatoryTransactionService {

    void add(Transaction tx, byte[] requiredTxHash);

}
