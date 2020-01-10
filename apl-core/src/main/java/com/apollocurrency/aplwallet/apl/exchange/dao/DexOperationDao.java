package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.mapper.DexOperationMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOperation;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;

public interface DexOperationDao {
    @Transactional(readOnly = true)
    @RegisterRowMapper(DexOperationMapper.class)
    @SqlQuery("SELECT * FROM dex_operation WHERE account = :account AND stage = :stage AND eid = :eid ORDER BY time DESC LIMIT 1")
    @RegisterArgumentFactory(DexOperationStageArgumentFactory.class)
    DexOperation getBy(@Bind("account") String account, @Bind("stage") DexOperation.Stage stage, @Bind("eid") String eid);

    @Transactional
    @SqlUpdate("INSERT INTO dex_operation (account, stage, description, details, time, finished) VALUES (:account, :stage, :description, :details, :time, :finished)")
    @RegisterArgumentFactory(DexOperationStageArgumentFactory.class)
    @GetGeneratedKeys
    long add(@BindBean DexOperation op);

    @Transactional
    @SqlUpdate("UPDATE dex_operation SET account = :account, stage = :stage, eid = :eid, description = :description,  details = :details, time = :time, finished = :finished WHERE db_id = :dbId")
    @RegisterArgumentFactory(DexOperationStageArgumentFactory.class)
    int updateByDbId(@BindBean DexOperation op);

    @Transactional
    @SqlUpdate("DELETE FROM dex_operation WHERE time < :toTimestamp")
    int deleteAfterTimestamp(@Bind("toTimestamp") Timestamp toTimestamp);

}
