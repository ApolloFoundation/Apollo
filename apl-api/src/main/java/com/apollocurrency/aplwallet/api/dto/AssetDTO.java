package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDTO {
    public String quantityATU;
    public long numberOfAccounts;
    public String accountRS;
    public long decimals;
    public long numberOfTransfers;
    public String name;
    public String description;
    public long numberOfTrades;
    public String asset;
    public String account;
}
