package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DexOrderWithFreezing {
    private DexOrder dexOrder;
    private boolean hasFrozenMoney;

}
