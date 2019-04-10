/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import java.math.BigDecimal;

public class DexOfferDBRequest {

    private OfferType type;
    private Integer currentTime;
    private DexCurrencies offerCur;
    private DexCurrencies pairCur;
    private BigDecimal minAskPrice;
    private BigDecimal maxBidPrice;

    public DexOfferDBRequest(OfferType type, Integer currentTime, DexCurrencies offerCur, DexCurrencies pairCur, BigDecimal minAskPrice, BigDecimal maxBidPrice) {
        this.type = type;
        this.currentTime = currentTime;
        this.offerCur = offerCur;
        this.pairCur = pairCur;
        this.minAskPrice = minAskPrice;
        this.maxBidPrice = maxBidPrice;
    }

    public DexOfferDBRequest() {
    }

    public OfferType getType() {
        return type;
    }

    public void setType(OfferType type) {
        this.type = type;
    }

    public DexCurrencies getOfferCur() {
        return offerCur;
    }

    public void setOfferCur(DexCurrencies offerCur) {
        this.offerCur = offerCur;
    }

    public DexCurrencies getPairCur() {
        return pairCur;
    }

    public void setPairCur(DexCurrencies pairCur) {
        this.pairCur = pairCur;
    }

    public BigDecimal getMinAskPrice() {
        return minAskPrice;
    }

    public void setMinAskPrice(BigDecimal minAskPrice) {
        this.minAskPrice = minAskPrice;
    }

    public BigDecimal getMaxBidPrice() {
        return maxBidPrice;
    }

    public void setMaxBidPrice(BigDecimal maxBidPrice) {
        this.maxBidPrice = maxBidPrice;
    }

    public Integer getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Integer currentTime) {
        this.currentTime = currentTime;
    }
}
