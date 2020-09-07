/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.attachment;

import com.apollocurrency.aplwallet.api.dto.AppendixDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonetarySystemCurrencyIssuanceDTO extends AppendixDTO {
    public String name;
    public String code;
    public String description;
    public byte type;
    public long initialSupply;
    public long reserveSupply;
    public long maxSupply;
    public int issuanceHeight;
    public long minReservePerUnitATM;
    public int minDifficulty;
    public int maxDifficulty;
    public byte ruleset;
    public byte algorithm;
    public byte decimals;
}
