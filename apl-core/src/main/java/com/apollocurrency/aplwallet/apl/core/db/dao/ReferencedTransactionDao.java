/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ReferencedTransaction;

import java.util.List;
import java.util.Optional;

public interface ReferencedTransactionDao {

    Optional<Long> getReferencedTransactionIdFor(long transactionId);

    List<Long> getAllReferencedTransactionIds();

    int save(ReferencedTransaction referencedTransaction);

    List<Transaction> getReferencingTransactions(long transactionId, int from, Integer limit);
}
