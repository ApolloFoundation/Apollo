/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;

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
    
}
