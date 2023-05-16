package com.apollocurrency.aplwallet.apl.dex.eth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EthDepositsWithOffset  extends OffsetModel {
    private List<EthDepositInfo> deposits = new ArrayList<>();
}
