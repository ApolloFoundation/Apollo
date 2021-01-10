package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderDbIdPaginationDbRequest {
    private long fromDbId;
    private DexCurrency coin;
    private int fromTime;
    private int toTime;
    private int limit;
}
