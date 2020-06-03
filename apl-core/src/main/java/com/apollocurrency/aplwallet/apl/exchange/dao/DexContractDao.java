/*
 * Copyright © 2018-2019 Apollo Foundation
 */


package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.converter.db.ExchangeContractMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
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
    @SqlQuery("SELECT * FROM dex_contract AS contract " +
        "where latest=true " +
        "AND (:recipient is NULL or contract.recipient=:recipient) " +
        "AND (:sender is NULL or contract.sender=:sender) " +
        "AND (:offerId is NULL or contract.offer_id=:offerId) " +
        "AND (:counterOfferId is NULL or contract.counter_offer_id=:counterOfferId) " +
        "AND contract.status IN (<statuses>) " +
        "ORDER BY contract.db_id desc")
    @RegisterRowMapper(ExchangeContractMapper.class)
    List<ExchangeContract> getAllWithMultipleStatuses(@BindBean DexContractDBRequest dexContractDBRequest, @BindList("statuses") List<Integer> statuses);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract " +
        "where latest=true " +
        "AND (:id is NULL or id=:id) " +
        "AND (:recipient is NULL or recipient=:recipient) " +
        "AND (:sender is NULL or sender=:sender) " +
        "AND (:offerId is NULL or offer_id=:offerId) " +
        "AND (:counterOfferId is NULL or counter_offer_id=:counterOfferId) " +
        "AND (:status is NULL or status=:status)")
    @RegisterRowMapper(ExchangeContractMapper.class)
    ExchangeContract get(@BindBean DexContractDBRequest dexContractDBRequest);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract  AS contract " +
        "where latest=true " +
        "AND (:id is NULL or id=:id) " +
        "AND (:recipient is NULL or recipient=:recipient) " +
        "AND (:sender is NULL or sender=:sender) " +
        "AND (:offerId is NULL or offer_id=:offerId) " +
        "AND (:counterOfferId is NULL or counter_offer_id=:counterOfferId) " +
        "AND contract.status IN (<statuses>) " +
        "ORDER BY contract.db_id desc")
    @RegisterRowMapper(ExchangeContractMapper.class)
    ExchangeContract getWithMultipleStatuses(@BindBean DexContractDBRequest dexContractDBRequest, @BindList("statuses") List<Integer> statuses);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_contract AS contract " +
        "WHERE latest=true " +
        "AND (recipient = :account or sender = :account) " +
        "AND (offer_id = :orderId or counter_offer_id=:orderId) " +
        "AND status BETWEEN :fromStatus AND :toStatus ORDER BY height desc, db_id desc")
    @RegisterRowMapper(ExchangeContractMapper.class)
    List<ExchangeContract> getAllForAccountOrder(@Bind("account") long account, @Bind("orderId") long orderId, @Bind("fromStatus") int fromStatus, @Bind("toStatus") int toStatus);

    @SqlQuery("SELECT * FROM dex_contract AS contract " +
        "WHERE (recipient = :account or sender = :account) " +
        "AND (offer_id = :orderId or counter_offer_id=:orderId) " +
        "AND status BETWEEN :fromStatus AND :toStatus ORDER BY height desc, db_id desc")
    @RegisterRowMapper(ExchangeContractMapper.class)
    List<ExchangeContract> getAllVersionedForAccountOrder(@Bind("account") long account, @Bind("orderId") long orderId, @Bind("fromStatus") int fromStatus, @Bind("toStatus") int toStatus);

}
