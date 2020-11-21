package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.mapper.DexOperationMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOperation;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;
import java.util.List;

public interface DexOperationDao {
    @Transactional(readOnly = true)
    @RegisterRowMapper(DexOperationMapper.class)
    @SqlQuery("SELECT * FROM dex_operation WHERE account = :account AND stage = :stage AND eid = :eid ORDER BY ts DESC LIMIT 1")
    @RegisterArgumentFactory(DexOperationStageArgumentFactory.class)
    DexOperation getBy(@Bind("account") String account, @Bind("stage") DexOperation.Stage stage, @Bind("eid") String eid);

    @Transactional
    @SqlUpdate("INSERT INTO dex_operation (account, stage, description, details, eid,  ts, finished) VALUES (:account, :stage, :description, :details,:eid, :ts, :finished)")
    @RegisterArgumentFactory(DexOperationStageArgumentFactory.class)
    @GetGeneratedKeys
    long add(@BindBean DexOperation op);

    @Transactional
    @SqlUpdate("UPDATE dex_operation SET account = :account, stage = :stage, eid = :eid, description = :description,  details = :details, ts = :ts, finished = :finished WHERE db_id = :dbId")
    @RegisterArgumentFactory(DexOperationStageArgumentFactory.class)
    int updateByDbId(@BindBean DexOperation op);

    @Transactional
    @SqlUpdate("DELETE FROM dex_operation WHERE ts < :toTimestamp")
    int deleteAfterTimestamp(@Bind("toTimestamp") Timestamp toTimestamp);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_operation WHERE db_id > :fromDbId ORDER BY db_id LIMIT :limit")
    @RegisterRowMapper(DexOperationMapper.class)
    List<DexOperation> getAll(@Bind("fromDbId") long fromDbId, @Bind("limit") int limit);

}
