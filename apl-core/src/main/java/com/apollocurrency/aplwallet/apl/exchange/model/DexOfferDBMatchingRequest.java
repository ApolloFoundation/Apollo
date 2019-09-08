/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 *
 * @author Serhiy Lymar
 */

@Getter @Setter
public class DexOfferDBMatchingRequest {

    public DexOfferDBMatchingRequest(OrderType type, Integer currentTime, Integer offerCur, BigDecimal offerAmount,
                                     Integer pairCur, BigDecimal pairRate, String order) {
        this.type = this.type = type != null ? type.ordinal() : null;
        this.currentTime = currentTime;
        this.offerCur = offerCur;                     
        this.offerAmount = offerAmount;
        this.pairCur = pairCur;
        this.pairRate = pairRate;
    }
    
    private Integer type;
    private Integer currentTime;
    private Integer offerCur;
    private BigDecimal offerAmount;
    private Integer pairCur;
    private BigDecimal pairRate;
           
}
