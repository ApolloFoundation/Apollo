/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 *
 * @author Serhiy Lymar
 */

@Data
public class DexOrderDBMatchingRequest {

    public DexOrderDBMatchingRequest(OrderType type, Integer currentTime, Integer offerCur, BigDecimal offerAmount,
                                     Integer pairCur, BigDecimal pairRate, String orderBy) {
        this.type = type != null ? type.ordinal() : null;
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
