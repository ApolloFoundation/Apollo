/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexOfferMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface DexOfferDao {

    @Transactional
    @SqlUpdate("INSERT INTO dex_offer (transaction_id, account_id, type, " +
            "offer_currency, offer_amount, pair_currency, pair_rate, finish_time, status)" +
            "VALUES (:transactionId, :accountId, :type, :offerCurrency, :offerAmount, :pairCurrency, :pairRate, :finishTime, :status)"
    )
    void save(@BindBean DexOffer dexOffer);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_offer AS offer " +
            "WHERE " +
            "(:accountId is NULL or offer.account_id = :accountId)" +
            "AND (:currentTime is NULL OR offer.finish_time > :currentTime) " +
            "AND (:type is NULL OR offer.type = :type) " +
            "AND (:status is NULL OR offer.status = :status) " +
            "AND (:offerCur is NULL OR offer.offer_currency = :offerCur) " +
            "AND (:pairCur is NULL OR offer.pair_currency = :pairCur) " +
            "ORDER BY offer.pair_rate DESC"
    )
    @RegisterRowMapper(DexOfferMapper.class)
    List<DexOffer> getOffers(@BindBean DexOfferDBRequest dexOfferDBRequest);

}
