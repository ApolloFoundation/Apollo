package com.apollocurrency.aplwallet.apl.dex.eth.model;

import com.apollocurrency.aplwallet.apl.dex.core.model.ExpiredSwap;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AddressEthExpiredSwaps {
    private String address;
    private List<ExpiredSwap> expiredSwaps;
}
