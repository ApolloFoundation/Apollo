package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
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
            "AND (:counterOfferId is NULL or contract.counter_offer_id=:counterOfferId) " +
            "AND (:status is NULL or contract.status=:status)")
    @RegisterRowMapper(ExchangeContractMapper.class)
    List<ExchangeContract> getAll(@BindBean DexContractDBRequest dexContractDBRequest);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract " +
            "where latest=true " +
            "AND (:recipient is NULL or recipient=:recipient) " +
            "AND (:sender is NULL or sender=:sender) " +
            "AND (:offerId is NULL or offer_id=:offerId) " +
            "AND (:counterOfferId is NULL or counter_offer_id=:counterOfferId) " +
            "AND (:status is NULL or status=:status)")
    @RegisterRowMapper(ExchangeContractMapper.class)
    ExchangeContract get(@BindBean DexContractDBRequest dexContractDBRequest);

}
