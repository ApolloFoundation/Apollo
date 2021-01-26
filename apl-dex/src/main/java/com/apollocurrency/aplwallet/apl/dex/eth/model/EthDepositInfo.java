package com.apollocurrency.aplwallet.apl.dex.eth.model;

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
