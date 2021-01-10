package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AddressEthDepositsInfo {
    private String address;
    private List<EthDepositInfo> depositsInfo;
}
