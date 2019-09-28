/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexOfferMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.AllowUnusedBindings;

public interface DexOfferDao {

    /**
     * Use save/insert in the DexOfferTable. To provide save rollback and versions.
     */
//    @Transactional
//    @SqlUpdate("INSERT INTO dex_offer (transaction_id, account_id, type, " +
//            "offer_currency, offer_amount, pair_currency, pair_rate, finish_time, status)" +
//            "VALUES (:transactionId, :accountId, :type, :offerCurrency, :offerAmount, :pairCurrency, :pairRate, :finishTime, :status)"
//    )
//    void save(@BindBean DexOffer dexOffer);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
            "WHERE latest = true " +
            "AND (:accountId is NULL or offer.account_id = :accountId) " +
            "AND (:currentTime is NULL OR offer.finish_time > :currentTime) " +
            "AND (:type is NULL OR offer.type = :type) " +
            "AND (:status is NULL OR offer.status = :status) " +
            "AND (:offerCur is NULL OR offer.offer_currency = :offerCur) " +
            "AND (:pairCur is NULL OR offer.pair_currency = :pairCur) " +
            "ORDER BY offer.pair_rate DESC " +
            "OFFSET :offset LIMIT :limit"
    )
    @RegisterRowMapper(DexOfferMapper.class)
    List<DexOffer> getOffers(@BindBean DexOfferDBRequest dexOfferDBRequest);

    @AllowUnusedBindings    
    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
            " WHERE latest = true" +
            " AND offer.type = :type" + 
            " AND offer.finish_time > :currentTime" +
            " AND offer.offer_currency = :offerCur" +
            " AND offer.offer_amount = :offerAmount" +
            " AND offer.pair_currency = :pairCur" + 
            " AND offer.pair_rate <= :pairRate" +
            " AND offer.status = 0" +
            " ORDER BY offer.pair_rate ASC")
    @RegisterRowMapper(DexOfferMapper.class)
    List<DexOffer> getOffersForMatching(@BindBean DexOfferDBMatchingRequest dexOfferDBMatchingRequest );

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer where latest = true AND transaction_id = :transactionId")
    @RegisterRowMapper(DexOfferMapper.class)
    DexOffer getByTransactionId(@Bind("transactionId") long transactionId);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer where latest = true AND db_id = :id")
    @RegisterRowMapper(DexOfferMapper.class)
    DexOffer getById(@Bind("id") long id);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
            " where latest = true " +
            " AND offer.finish_time < :currentTime" +
            " AND offer.status = 0")
    @RegisterRowMapper(DexOfferMapper.class)
    List<DexOffer> getOverdueOrders(@Bind("currentTime") int currentTime);

}
