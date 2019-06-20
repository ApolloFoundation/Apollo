/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import java.math.BigDecimal;

/**
 *
 * @author Serhiy Lymar
 */
public class DexOfferDBMatchingRequest {

    public DexOfferDBMatchingRequest(Integer type, Integer currentTime, Integer offerCur, BigDecimal offerAmount, Integer pairCur, BigDecimal pairRate) {
        this.type = type;
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

    public Integer getType() {
        return type;
    }

    public Integer getCurrentTime() {
        return currentTime;
    }

    public Integer getOfferCur() {
        return offerCur;
    }

    public BigDecimal getOfferAmount() {
        return offerAmount;
    }

    public Integer getPairCur() {
        return pairCur;
    }

    public BigDecimal getPairRate() {
        return pairRate;
    }
        
    public void setType(Integer type) {
        this.type = type;
    }

    public void setCurrentTime(Integer currentTime) {
        this.currentTime = currentTime;
    }

    public void setOfferCur(Integer offerCur) {
        this.offerCur = offerCur;
    }

    public void setOfferAmount(BigDecimal offerAmount) {
        this.offerAmount = offerAmount;
    }

    public void setPairCur(Integer pairCur) {
        this.pairCur = pairCur;
    }

    public void setPairRate(BigDecimal pairRate) {
        this.pairRate = pairRate;
    }
    
}
