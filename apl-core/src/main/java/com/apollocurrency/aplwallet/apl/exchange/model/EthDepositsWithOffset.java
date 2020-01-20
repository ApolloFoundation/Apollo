package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EthDepositsWithOffset {
    private List<UserEthDepositInfo> deposits;
    private Long offset;
}
