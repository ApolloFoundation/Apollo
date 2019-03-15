/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface ReferencedTransactionDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT referenced_transaction_id FROM referenced_transaction where transaction_id = :transactionId")
    Long getReferencedTransactionIdFor(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT referenced_transaction_id FROM referenced_transaction")
    List<Long> getAllReferencedTransactionIds();
}
