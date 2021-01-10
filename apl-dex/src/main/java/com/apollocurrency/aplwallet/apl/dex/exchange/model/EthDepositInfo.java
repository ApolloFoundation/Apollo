package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EthDepositInfo {
    private Long orderId;

    private BigDecimal amount;

    private Long creationTime;
}
