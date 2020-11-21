package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AddressEthExpiredSwaps {
    private String address;
    private List<ExpiredSwap> expiredSwaps;
}
