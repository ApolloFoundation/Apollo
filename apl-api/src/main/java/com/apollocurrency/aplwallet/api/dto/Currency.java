/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Currency extends ResponseBase {
    private long accountId;
    private String name;
    private String code;
    private String accountRS;
    private String description;
    private int type;
    private List<CurrencyTypes> types;
    private long maxSupply;
    private long reserveSupply;
    private int creationHeight;
    private int issuanceHeight;
    private long minReservePerUnitATM;
    private int minDifficulty;
    private int maxDifficulty;
    private byte ruleset;
    private byte algorithm;
    private byte decimals;
    private long initialSupply;
    private long currentSupply;
    private long units;
    private long unconfirmedUnits;
    private String account;
    private long currentReservePerUnitATM;
    private String currency;

}
