package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface DexContractDao {

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract AS contract " +
            "where latest=true " +
            "AND (:recipient is NULL or contract.recipient=:recipient) " +
            "AND (:sender is NULL or contract.sender=:sender) " +
            "AND (:offerId is NULL or contract.offer_id=:offerId) " +
            "AND contract.status=:status")
    @RegisterRowMapper(ExchangeContractMapper.class)
    List<ExchangeContract> getAll(@BindBean DexContractDBRequest dexContractDBRequest);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract " +
            "where latest=true " +
            "AND (:recipient is NULL or recipient=:recipient) " +
            "AND (:sender is NULL or sender=:sender) " +
            "AND (:offerId is NULL or offer_id=:offerId) " +
            "AND status=:status")
    @RegisterRowMapper(ExchangeContractMapper.class)
    ExchangeContract get(@BindBean DexContractDBRequest dexContractDBRequest);


    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract " +
            "WHERE latest=true AND offer_id=:offerId AND sender=:sender " +
            "AND status=:status")
    @AllowUnusedBindings
    @RegisterRowMapper(ExchangeContractMapper.class)
    ExchangeContract getBySender(@Bind("sender") long sender, @Bind("offerId") long offerId, @Bind("status") Integer status);


}
