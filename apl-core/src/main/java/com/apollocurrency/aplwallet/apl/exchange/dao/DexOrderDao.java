/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.converter.db.DexOrderMapper;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.model.DBSortOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequestForTrading;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderSortBy;
import com.apollocurrency.aplwallet.apl.exchange.model.HeightDbIdRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderDbIdPaginationDbRequest;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

/**
 * Use save/insert in the DexOfferTable. To provide save rollback and versions.
 */
public interface DexOrderDao {


    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
        "WHERE latest = true " +
        "AND (offer.db_id > :dbId) " +
        "AND (:accountId is NULL or offer.account_id = :accountId) " +
        "AND (:currentTime is NULL OR offer.finish_time > :currentTime) " +
        "AND (:type is NULL OR offer.type = :type) " +
        "AND (:status is NULL OR offer.status = :status) " +
        "AND (:offerCur is NULL OR offer.offer_currency = :offerCur) " +
        "AND (:pairCur is NULL OR offer.pair_currency = :pairCur) " +
        "ORDER BY <sortBy> <sortOrder> " +
        "OFFSET :offset FETCH FIRST :limit ROWS ONLY"
    )
    @RegisterRowMapper(DexOrderMapper.class)
    List<DexOrder> getOrders(@BindBean DexOrderDBRequest dexOrderDBRequest, @Define("sortBy") DexOrderSortBy sortBy, @Define("sortOrder") DBSortOrder sortOrder);

    @AllowUnusedBindings
    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
        " WHERE latest = true" +
        " AND offer.type = :type" +
        " AND offer.finish_time > :currentTime" +
        " AND offer.offer_currency = :offerCur" +
        " AND offer.offer_amount = :offerAmount" +
        " AND offer.pair_currency = :pairCur" +
        " AND offer.pair_rate = :pairRate" +
        " AND offer.status = 0" +
        " ORDER BY offer.pair_rate <orderby> ")
    @RegisterRowMapper(DexOrderMapper.class)
    List<DexOrder> getOffersForMatchingPure(@BindBean DexOrderDBMatchingRequest dexOrderDBMatchingRequest, @Define("orderby") String orderBy);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
        "WHERE latest = true " +
        "AND offer.status = 5 " + // CLOSED
        "AND offer.type = 0 " + // only autocloseable buy orders
        "AND offer.pair_currency = :coin " +
        "AND offer.height < :toHeight " +
        "AND offer.db_id > :fromDbId ORDER BY db_id " +
        "LIMIT :limit")
    @RegisterRowMapper(DexOrderMapper.class)
    List<DexOrder> getClosedOrdersFromDbId(@BindBean HeightDbIdRequest heightDbIdRequest);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
        "WHERE latest = true " +
        "AND offer.status = 5 " + // CLOSED
        "AND offer.type = 0 " + // only autocloseable buy orders
        "AND offer.pair_currency = :coin " +
        "AND offer.height < :toHeight " +
        " ORDER BY height DESC, db_id DESC " +
        "LIMIT 1")
    @RegisterRowMapper(DexOrderMapper.class)
    DexOrder getLastClosedOrderBeforeHeight(@Bind("coin") DexCurrency coin, @Bind("toHeight") int toHeight);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
        "WHERE latest = true " +
        "AND offer.status = 5 " + // CLOSED
        "AND offer.type = 0 " + // only autocloseable buy orders
        "AND offer.pair_currency = :coin " +
        "AND offer.finish_time < :timestamp " +
        " ORDER BY finish_time DESC " +
        "LIMIT 1")
    @RegisterRowMapper(DexOrderMapper.class)
    DexOrder getLastClosedOrderBeforeTimestamp(@Bind("coin") DexCurrency coin, @Bind("timestamp") int timestamp);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
        "WHERE latest = true " +
        "AND (offer.finish_time > :startInterval) " +
        "AND (offer.finish_time <= :endInterval) " +
        "AND (offer.type = :requestedType) " +
        "AND (offer.status = 5) " +
        "AND (offer.pair_currency = :pairCur) " +
        "ORDER BY offer.height ASC " +
        "LIMIT :limit OFFSET :offset  "
    )
    @RegisterRowMapper(DexOrderMapper.class)
    List<DexOrder> getOrdersForTrading(@BindBean DexOrderDBRequestForTrading dexOrderDBRequestForTrading);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
        "WHERE latest = true " +
        "AND offer.finish_time BETWEEN :fromTime AND :toTime " +
        "AND offer.db_id > :fromDbId " + // pagination
        "AND offer.type = 0 " +
        "AND offer.status = 5 " +
        "AND offer.pair_currency = :coin " +
        "ORDER BY offer.db_id ASC " +
        "LIMIT :limit "
    )
    @RegisterRowMapper(DexOrderMapper.class)
    List<DexOrder> getOrdersFromDbIdBetweenTimestamps(@BindBean OrderDbIdPaginationDbRequest request);

}
