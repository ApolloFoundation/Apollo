package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EthDepositsWithOffset {
    private List<EthDepositInfo> deposits = new ArrayList<>();
    private long offset;
}
