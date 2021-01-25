package com.apollocurrency.aplwallet.apl.dex.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderFreezing {
    private long id;
    private boolean hasFrozenMoney;

}
