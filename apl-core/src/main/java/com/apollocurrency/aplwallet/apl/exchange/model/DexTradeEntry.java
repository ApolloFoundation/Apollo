/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data class for interaction with trade table in the database
 * @author Serhiy Lymar
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class DexTradeEntry {

    private long dbId;
    private long transactionID;
    private long senderOfferID; 
    private long receiverOfferID;
    private byte senderOfferType; 
    private byte senderOfferCurrency; 
    private long senderOfferAmount;
    private byte pairCurrency;
    private BigDecimal pairRate;
    private Integer finishTime;
    private Integer height; 
}
