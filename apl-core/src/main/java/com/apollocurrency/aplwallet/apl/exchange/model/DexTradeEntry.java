/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data class for interaction with trade table in the database
 * @author Serhiy Lymar
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class DexTradeEntry extends DerivedEntity {

    private long transactionID;
    private long senderOfferID; 
    private long receiverOfferID;
    private byte senderOfferType; 
    private byte senderOfferCurrency; 
    private long senderOfferAmount;
    private byte pairCurrency;
    private BigDecimal pairRate;
    private Integer finishTime;

    public DexTradeEntry(Long dbId, Integer height) {
        super(dbId, height);
    }

    public DexTradeEntry(ResultSet rs) throws SQLException {
        super(rs);
    }

    @Builder(builderMethodName = "builder")
    public DexTradeEntry(Long dbId, Integer height, long transactionID, long senderOfferID,
                         long receiverOfferID, byte senderOfferType, byte senderOfferCurrency, long senderOfferAmount,
                         byte pairCurrency, BigDecimal pairRate, Integer finishTime) {
        super(dbId, height);
        this.transactionID = transactionID;
        this.senderOfferID = senderOfferID;
        this.receiverOfferID = receiverOfferID;
        this.senderOfferType = senderOfferType;
        this.senderOfferCurrency = senderOfferCurrency;
        this.senderOfferAmount = senderOfferAmount;
        this.pairCurrency = pairCurrency;
        this.pairRate = pairRate;
        this.finishTime = finishTime;
    }
}
