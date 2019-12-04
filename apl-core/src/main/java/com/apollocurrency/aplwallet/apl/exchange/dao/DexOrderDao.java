/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexOrderMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrderDBRequestForTrading;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;
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
            "ORDER BY offer.pair_rate DESC " +
            "OFFSET :offset LIMIT :limit"
    )
    @RegisterRowMapper(DexOrderMapper.class)
    List<DexOrder> getOrders(@BindBean DexOrderDBRequest dexOrderDBRequest);

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
            "AND (offer.finish_time > :startInterval) " +
            "AND (offer.finish_time <= :endInterval) " +
            "AND (offer.type = :requestedType) " +
            "AND (offer.status = 5) " +            
            "AND (offer.pair_currency = :pairCur) " +            
            "ORDER BY offer.finish_time ASC " +
            "OFFSET :offset LIMIT :limit "             
    )
    @RegisterRowMapper(DexOrderMapper.class)
    List<DexOrder> getOrdersForTrading(@BindBean DexOrderDBRequestForTrading dexOrderDBRequestForTrading);
   
}
