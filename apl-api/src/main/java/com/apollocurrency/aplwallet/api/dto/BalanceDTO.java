package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDTO {
    public long balanceATM;
    public long forgedBalanceATM;
    public long requestProcessingTime;
    public long unconfirmedBalanceATM;
    public long guaranteedBalanceATM;

}
