/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import java.math.BigDecimal;

public class DexOfferDBRequest {

    private Integer type;
    private Long accountId;
    private Integer currentTime;
    private Integer offerCur;
    private Integer pairCur;
    private OfferStatus status;
    private BigDecimal minAskPrice;
    private BigDecimal maxBidPrice;

    public DexOfferDBRequest(OfferType type, Integer currentTime, DexCurrencies offerCur, DexCurrencies pairCur, Long accountId, OfferStatus status, BigDecimal minAskPrice, BigDecimal maxBidPrice) {
        this.type = type != null ? type.ordinal() : null;
        this.currentTime = currentTime;
        this.offerCur = offerCur != null ? offerCur.ordinal() : null;
        this.pairCur = pairCur != null ? pairCur.ordinal() : null;
        this.minAskPrice = minAskPrice;
        this.maxBidPrice = maxBidPrice;
        this.accountId = accountId;
        this.status = status;
    }

    public DexOfferDBRequest() {
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getOfferCur() {
        return offerCur;
    }

    public void setOfferCur(Integer offerCur) {
        this.offerCur = offerCur;
    }

    public Integer getPairCur() {
        return pairCur;
    }

    public void setPairCur(Integer pairCur) {
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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }
}
