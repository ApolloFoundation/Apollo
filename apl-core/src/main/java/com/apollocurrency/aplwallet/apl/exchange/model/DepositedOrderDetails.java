package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
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
