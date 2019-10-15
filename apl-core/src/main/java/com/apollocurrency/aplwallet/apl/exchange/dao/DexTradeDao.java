/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.DexTradeEntryMapper;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import java.util.List;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * DAO for interaction with Trade Table of the database
 * @author Serhiy Lymar
 */
public interface  DexTradeDao {
       
    @Transactional
    @SqlUpdate("INSERT INTO dex_trade(TRANSACTION_ID, "
            + "SENDER_OFFER_ID, RECEIVER_OFFER_ID, SENDER_OFFER_TYPE, "
            + "SENDER_OFFER_CURRENCY, SENDER_OFFER_AMOUNT, PAIR_CURRENCY, "
            + "PAIR_RATE, FINISH_TIME, HEIGHT ) "
            + "VALUES (:transactionID, :senderOfferID, :receiverOfferID, "
            + ":senderOfferType, :senderOfferCurrency, :senderOfferAmount, "
            + ":pairCurrency, :pairRate, :finishTime, :height )")
    void saveDexTradeEntry(@BindBean DexTradeEntry dexTradeEntry);
    
    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM dex_trade as tradeinfo WHERE tradeinfo.finish_time >= :start AND tradeinfo.finish_time < :finish AND tradeinfo.pair_currency = :pairCurrency OFFSET :offset LIMIT :limit")
    @RegisterRowMapper(DexTradeEntryMapper.class)
    List<DexTradeEntry> getDexEntriesForInterval(@Bind("start") Integer start, @Bind("finish") Integer finish, @Bind("pairCurrency") Byte pairCurrency,
            @Bind("offset") Integer offset, @Bind("limit") Integer limit);
    
}
