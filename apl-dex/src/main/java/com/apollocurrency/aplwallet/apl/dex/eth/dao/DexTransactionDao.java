package com.apollocurrency.aplwallet.apl.dex.eth.dao;

import com.apollocurrency.aplwallet.apl.dex.exchange.mapper.DexTransactionMapper;
import com.apollocurrency.aplwallet.apl.dex.exchange.mapper.OpArgumentFactory;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.DexTransaction;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
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
    @RegisterArgumentFactory(OpArgumentFactory.class)
    DexTransaction get(@Bind("params") String params, @Bind("account") String account, @Bind("operation") DexTransaction.Op operation);

    @Transactional(readOnly = true)
    @RegisterRowMapper(DexTransactionMapper.class)
    @SqlQuery("SELECT * FROM dex_transaction WHERE db_id = :dbId")
    @RegisterArgumentFactory(OpArgumentFactory.class)
    DexTransaction get(@Bind("dbId") long dbId);

    @Transactional
    @SqlUpdate("INSERT INTO dex_transaction (hash, tx, operation, params, account, timestamp) VALUES (:hash, :rawTransactionBytes, :operation, :params, :account, :timestamp)")
    @RegisterArgumentFactory(OpArgumentFactory.class)
    void add(@BindBean DexTransaction tx);

    @Transactional
    @SqlUpdate("UPDATE dex_transaction SET hash = :hash, tx = :rawTransactionBytes, timestamp = :timestamp WHERE params =:params AND account =:account AND operation=:operation")
    @RegisterArgumentFactory(OpArgumentFactory.class)
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
