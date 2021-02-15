package com.apollocurrency.aplwallet.apl.dex.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class DepositedOrderDetails {
    private boolean created;
    private String assetAddress;
    /**
     * Eth
     */
    private BigDecimal amount;
    /**
     * true if deposit was withdrawn.
     */
    private boolean withdrawn;
}
