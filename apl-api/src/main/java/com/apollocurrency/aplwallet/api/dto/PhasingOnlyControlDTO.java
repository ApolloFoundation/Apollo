package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhasingOnlyControlDTO {
    public long minDuration;
    public long votingModel;
    public String minBalance;
    public String accountRS;
    public String quorum;
    public String maxFees;
    public WhitelistDTO[] whitelistDTO;
    public long minBalanceModel;
    public String account;
    public long maxDuration;
}
