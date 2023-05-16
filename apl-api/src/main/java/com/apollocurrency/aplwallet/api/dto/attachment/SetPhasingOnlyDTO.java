/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.attachment;

import com.apollocurrency.aplwallet.api.dto.AppendixDTO;
import com.apollocurrency.aplwallet.api.dto.account.PhasingParamsDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetPhasingOnlyDTO extends AppendixDTO {
    @JsonProperty("controlMaxFees")
    public long maxFees;
    @JsonProperty("controlMinDuration")
    public short minDuration;
    @JsonProperty("controlMaxDuration")
    public short maxDuration;
    @JsonProperty("phasingControlParams")
    public PhasingParamsDTO phasingParams;
}
