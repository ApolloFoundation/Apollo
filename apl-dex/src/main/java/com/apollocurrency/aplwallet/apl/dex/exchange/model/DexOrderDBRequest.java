/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DexOrderDBRequest {
    private long dbId;
    private Integer type;
    private Long accountId;
    private Integer currentTime;
    private Integer offerCur;
    private Integer pairCur;
    private OrderStatus status;
    private BigDecimal minAskPrice;
    private BigDecimal maxBidPrice;
    private Integer offset;
    private Integer limit;
    private Boolean hasFrozenMoney;

    private DexOrderSortBy sortBy = DexOrderSortBy.PAIR_RATE;
    private DBSortOrder sortOrder = DBSortOrder.DESC;
}
