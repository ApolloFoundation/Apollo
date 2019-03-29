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

public interface ReferencedTransactionDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT referenced_transaction_id FROM referenced_transaction where transaction_id = :transactionId UNION " +
            "SELECT referenced_transaction_id FROM referenced_shard_transaction where transaction_id = :transactionId")
    Long getReferencedTransactionIdFor(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT referenced_transaction_id FROM referenced_transaction UNION " +
            "SELECT referenced_transaction_id FROM referenced_shard_transaction")
    List<Long> getAllReferencedTransactionIds();

    @Transactional
    @SqlUpdate("INSERT INTO referenced_transaction (transaction_id, referenced_transaction_id) VALUES (:transactionId, :referencedTransactionId)")
    int save(@BindBean ReferencedTransaction referencedTransaction);

    @Transactional(readOnly = true)
    @RegisterRowMapper(TransactionRowMapper.class)
    @SqlQuery("SELECT transaction.* FROM transaction, referenced_transaction "
            + "WHERE referenced_transaction.referenced_transaction_id = :transactionId "
            + "AND referenced_transaction.transaction_id = transaction.id "
            + "ORDER BY transaction.block_timestamp DESC, transaction.transaction_index DESC "
            + "OFFSET :from LIMIT :limit")
    List<Transaction> getReferencingTransactions(@Bind("transactionId") long transactionId, @Bind("from") int from, @Bind("limit") Integer limit);
}
