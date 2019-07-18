package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface DexContractDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract " +
            "where latest=true AND recipient=:recipient " +
            "AND status=0")
    @AllowUnusedBindings
    @RegisterRowMapper(ExchangeContractMapper.class)
    List<ExchangeContract> getByRecipient(@Bind("recipient") long recipient);

}
