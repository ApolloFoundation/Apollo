/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransactionEntity;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface MandatoryTransactionDao {
    @Transactional(readOnly = true)
    @SqlQuery("SELECT * from mandatory_transaction where id = :id")
    @RegisterRowMapper(MandatoryTransactionRowMapper.class)
    MandatoryTransactionEntity get(@Bind("id") long id);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * from mandatory_transaction where db_id > :fromDbId LIMIT :limit")
    @RegisterRowMapper(MandatoryTransactionRowMapper.class)
    List<MandatoryTransactionEntity> getAll(@Bind("fromDbId") long fromDbId, @Bind("limit") int limit);

    @Transactional
    @SqlUpdate("INSERT into mandatory_transaction(id, transaction_bytes, required_tx_hash) VALUES (:getId, :getTransactionBytes, :getRequiredTxHash)")
    void insert(@BindMethods MandatoryTransactionEntity mandatoryTransaction);

    @Transactional
    @SqlUpdate("DELETE from mandatory_transaction WHERE id = :id")
    int delete(@Bind("id") long id);

    @Transactional
    @SqlUpdate("DELETE FROM mandatory_transaction")
    int deleteAll();
}
