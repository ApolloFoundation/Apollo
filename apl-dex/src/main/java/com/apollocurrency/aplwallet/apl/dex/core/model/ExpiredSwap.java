package com.apollocurrency.aplwallet.apl.dex.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpiredSwap {
    private long orderId;
    private byte[] secretHash;
}
