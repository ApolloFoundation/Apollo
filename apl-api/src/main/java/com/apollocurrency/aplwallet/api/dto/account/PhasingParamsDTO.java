/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhasingParamsDTO {
    @JsonProperty("phasingQuorum")
    public long quorum;
    public long phasingMinBalance;
    public long phasingVotingModel;
    public String phasingHolding;
    public byte phasingMinBalanceModel;
    @JsonProperty("phasingWhitelist")
    public List<String> whitelist = new ArrayList<>();
}
