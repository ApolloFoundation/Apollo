/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
//@Builder
@NoArgsConstructor
public class DexOfferDBRequest {

    private Integer type;
    private Long accountId;
    private Integer currentTime;
    private Integer offerCur;
    private Integer pairCur;
    private OfferStatus status;
    private BigDecimal minAskPrice;
    private BigDecimal maxBidPrice;
    private Integer offset;
    private Integer limit;

    public DexOfferDBRequest(OfferType type, Integer currentTime, DexCurrencies offerCur, DexCurrencies pairCur, Long accountId, OfferStatus status,
                             BigDecimal minAskPrice, BigDecimal maxBidPrice, Integer offset, Integer limit) {
        this.type = type != null ? type.ordinal() : null;
        this.currentTime = currentTime;
        this.offerCur = offerCur != null ? offerCur.ordinal() : null;
        this.pairCur = pairCur != null ? pairCur.ordinal() : null;
        this.minAskPrice = minAskPrice;
        this.maxBidPrice = maxBidPrice;
        this.accountId = accountId;
        this.status = status;
        this.offset = offset;
        this.limit = limit;
    }

}
