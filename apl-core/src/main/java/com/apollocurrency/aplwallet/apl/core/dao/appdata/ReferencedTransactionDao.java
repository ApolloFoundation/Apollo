/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;

import java.util.List;
import java.util.Optional;

public interface ReferencedTransactionDao {

    Optional<Long> getReferencedTransactionIdFor(long transactionId);

    List<Long> getAllReferencedTransactionIds();

    int save(ReferencedTransaction referencedTransaction);

    void insert(ReferencedTransaction referencedTransaction);

    List<Transaction> getReferencingTransactions(long transactionId, int from, Integer limit);
}
