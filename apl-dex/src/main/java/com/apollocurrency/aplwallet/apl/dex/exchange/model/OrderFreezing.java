package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderFreezing {
    private long id;
    private boolean hasFrozenMoney;

}
