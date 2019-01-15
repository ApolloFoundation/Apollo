package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDTO {
    public String balanceATM;
    public String forgedBalanceATM;
    public String accountRS;
    public String publicKey;
    public String unconfirmedBalanceATM;
    public String account;
    public long numberOfBlocks;
    public long requestProcessingTime;
    public String name;
}

