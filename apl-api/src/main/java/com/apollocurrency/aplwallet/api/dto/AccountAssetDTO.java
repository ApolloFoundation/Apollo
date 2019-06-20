/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

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
@Getter @Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountAssetDTO {

    private String account;
    private String accountRS;
    private String name;
    private String description;
    private String asset;
    private Byte decimals;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long initialQuantityATU;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long quantityATU;

    private Integer numberOfTrades;
    private Integer numberOfTransfers;
    private Integer numberOfAccounts;

}
