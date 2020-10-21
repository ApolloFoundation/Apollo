/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.MandatoryTransaction;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface MandatoryTransactionDao {
    @Transactional(readOnly = true)
    @SqlQuery("SELECT * from mandatory_transaction where id = :id")
    @RegisterRowMapper(MandatoryTransactionMapper.class)
    MandatoryTransaction get(@Bind("id") long id);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * from mandatory_transaction where db_id > :fromDbId LIMIT :limit")
    @RegisterRowMapper(MandatoryTransactionMapper.class)
    List<MandatoryTransaction> getAll(@Bind("fromDbId") long fromDbId, @Bind("limit") int limit);

    @Transactional
    @SqlUpdate("INSERT into mandatory_transaction(id, transaction_bytes, required_tx_hash) VALUES (:getId, :getCopyTxBytes, :getRequiredTxHash)")
    void insert(@BindMethods MandatoryTransaction mandatoryTransaction);

    @Transactional
    @SqlUpdate("DELETE from mandatory_transaction WHERE id = :id")
    int delete(@Bind("id") long id);

    @Transactional
    @SqlUpdate("DELETE FROM mandatory_transaction")
    int deleteAll();
}
