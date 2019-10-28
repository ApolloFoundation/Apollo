package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.mapper.DexTransactionMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface DexTransactionDao {
    @Transactional(readOnly = true)
    @RegisterRowMapper(DexTransactionMapper.class)
    @SqlQuery("SELECT * FROM dex_transaction WHERE params =:params AND account =:account AND operation=:operation")
    @RegisterArgumentFactory(DexOperationArgumentFactory.class)
    DexTransaction get(@Bind("params") String params, @Bind("account") String account, @Bind("operation") DexTransaction.DexOperation operation);

    @Transactional(readOnly = true)
    @RegisterRowMapper(DexTransactionMapper.class)
    @SqlQuery("SELECT * FROM dex_transaction WHERE db_id = :dbId")
    @RegisterArgumentFactory(DexOperationArgumentFactory.class)
    DexTransaction get(@Bind("dbId") long dbId);

    @Transactional
    @SqlUpdate("INSERT INTO dex_transaction (hash, tx, operation, params, account, timestamp) VALUES (:hash, :rawTransactionBytes, :operation, :params, :account, :timestamp)")
    @RegisterArgumentFactory(DexOperationArgumentFactory.class)
    void add(@BindBean DexTransaction tx);

    @Transactional
    @SqlUpdate("UPDATE dex_transaction SET hash = :hash, tx = :rawTransactionBytes, timestamp = :timestamp WHERE params =:params AND account =:account AND operation=:operation")
    @RegisterArgumentFactory(DexOperationArgumentFactory.class)
    void update(@BindBean DexTransaction tx);

    @Transactional
    @SqlUpdate("DELETE FROM dex_transaction WHERE timestamp < :timestamp")
    void deleteAllBeforeTimestamp(@Bind("timestamp") long timestamp);

    @Transactional
    @SqlUpdate("DELETE FROM dex_transaction WHERE db_id = :dbId")
    void delete(@Bind("dbId") long dbId);

    @Transactional
    @RegisterRowMapper(DexTransactionMapper.class)
    @SqlQuery("SELECT * FROM dex_transaction WHERE db_id > :dbId LIMIT :limit")
    List<DexTransaction> getAll(@Bind("dbId") long fromDbId, @Bind("limit") int limit);

}
