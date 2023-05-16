package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.model.MandatoryTransaction;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;

import java.util.List;

public interface MandatoryTransactionService {

    List<MandatoryTransaction> getAll(long from, int limit);

    void saveMandatoryTransaction(Transaction tx, byte[] requiredTxHash);

    int clearAll();

    boolean deleteById(long id);
}
