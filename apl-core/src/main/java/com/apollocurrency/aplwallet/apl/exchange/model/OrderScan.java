package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderScan {
    private DexCurrency coin;
    private int lastTimestamp; // unix epoch seconds for 
    private long lastDbId; // last processed order db_id
}
