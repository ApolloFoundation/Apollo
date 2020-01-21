package com.apollocurrency.aplwallet.api.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Deprecated
@Getter
@Setter
public class SSEDataHolder{
    private List<JSONTransaction> transactions;
    private int aliasCount;
    private List<DGSPurchase> purchases;
    private int purchaseCount;
    private int messageCount;
    private int currencyCount;
    private List<AccountAssetDTO> assets;
    private AccountDTO account;
    private List<AccountCurrencyDTO> currencies;
    private BlockDTO blockDTO;

}