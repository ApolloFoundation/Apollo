package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.mapper.DexOperationMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOperation;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;

public interface DexOperationDao {
    @Transactional(readOnly = true)
    @RegisterRowMapper(DexOperationMapper.class)
    @SqlQuery("SELECT * FROM dex_operation WHERE account =:account AND details =:details ORDER BY time DESC LIMIT 1")
    DexOperation getByAccountDetails(@Bind("account") String account, @Bind("details") String details);

    @Transactional
    @SqlUpdate("INSERT INTO dex_operation (account, stage, description, details, time) VALUES (:account, :stage, :description, :details, :time)")
    DexOperation add(@BindBean DexOperation op);


    @Transactional
    @SqlUpdate("DELETE FROM dex_operation WHERE time < :toTimestamp")
    int deleteAfterTimestamp(Timestamp toTimestamp);

}
