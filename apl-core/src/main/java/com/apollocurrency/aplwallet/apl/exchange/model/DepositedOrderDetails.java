package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Data
@Builder
public class DepositedOrderDetails {
    private boolean created;
    private String assetAddress;
    private BigInteger amount;
    /**
     * true if deposit was withdrawn.
     */
    private boolean withdrawn;
}
