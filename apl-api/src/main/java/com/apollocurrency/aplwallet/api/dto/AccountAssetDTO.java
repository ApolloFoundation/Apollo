/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author <andrew.zinchenko@gmail.com>
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountAssetDTO extends BaseDTO {
    private String account;
    private String accountRS;
    @JsonIgnore
    private Long assetId;
    private String asset;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unconfirmedQuantityATU;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long quantityATU;

    //from Asset
    private String name;
    private String description;
    private Byte decimals;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long initialQuantityATU;

    private Integer numberOfTrades;
    private Integer numberOfTransfers;
    private Integer numberOfAccounts;

}
