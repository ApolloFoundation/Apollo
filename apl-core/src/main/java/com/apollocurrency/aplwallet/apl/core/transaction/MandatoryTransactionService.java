package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransaction;

import java.util.List;

public interface MandatoryTransactionService {

    List<MandatoryTransaction> getAll(long from, int limit);

    void add(Transaction tx, byte[] requiredTxHash);

    int clearAll();

    boolean deleteById(long id);
}
