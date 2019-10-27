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
    @SqlQuery("SELECT * FROM dex_transaction WHERE params =:params AND address =:address AND operation=:operation")
    @RegisterArgumentFactory(DexOperationArgumentFactory.class)
    DexTransaction get(@Bind("params") String params, @Bind("address") String address, @Bind("operation") DexTransaction.DexOperation operation);

    @Transactional
    @SqlUpdate("INSERT INTO dex_transaction (hash, tx, operation, params, address, timestamp) VALUES (:hash, :rawTransactionBytes, :operation, :params, :address, :timestamp")
    @RegisterArgumentFactory(DexOperationArgumentFactory.class)
    void add(@BindBean DexTransaction tx);

    @Transactional
    @SqlUpdate("UPDATE dex_transaction SET hash = :hash, tx = :rawTransactionBytes, timestamp :=address WHERE params =:params AND address =:address AND operation=:operation")
    @RegisterArgumentFactory(DexOperationArgumentFactory.class)
    void update(@BindBean DexTransaction tx);

    @Transactional
    @SqlUpdate("DELETE FROM dex_transaction WHERE timestamp < :timestamp")
    void deleteAllBeforeTimestamp(@Bind("timestamp") long timestamp);

    @Transactional
    @SqlUpdate("DELETE FROM dex_transaction WHERE db_id = :dbid")
    void delete(@Bind("dbId") long dbId);

    @Transactional
    @SqlQuery("SELECT * FROM dex_transaction WHERE db_id > :dbId LIMIT :limit")
    List<DexTransaction> getAll(@Bind("dbId") long fromDbId, @Bind("limit") int limit);

}
