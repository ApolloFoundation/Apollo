/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author <andrew.zinchenko@gmail.com>
 */

@Getter @Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountCurrencyDTO extends BaseDTO {
    private String account;
    private String accountRS;

    private String currency;
    private String units;
    private String unconfirmedUnits;

    private String name;
    private String code;
    private Integer type;
    private Byte decimals;
    private Integer issuanceHeight;
    private String issuerAccount;
    private String issuerAccountRS;
}
