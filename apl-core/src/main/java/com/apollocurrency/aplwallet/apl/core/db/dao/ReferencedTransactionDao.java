/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ReferencedTransaction;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

public interface ReferencedTransactionDao {

    Optional<Long> getReferencedTransactionIdFor(long transactionId);

    List<Long> getAllReferencedTransactionIds();

    int save(ReferencedTransaction referencedTransaction);

    List<Transaction> getReferencingTransactions(long transactionId, int from, Integer limit);
}
