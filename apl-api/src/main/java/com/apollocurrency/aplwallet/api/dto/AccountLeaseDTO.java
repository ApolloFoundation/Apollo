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
public class AccountLeaseDTO {
    @JsonIgnore
    private String account;
    @JsonIgnore
    private String accountRS;

    private String currentLessee;
    private String currentLesseeRS;
    private Integer currentHeightFrom;
    private Integer currentHeightTo;

    private String nextLessee;
    private String nextLesseeRS;
    private Integer nextHeightFrom;
    private Integer nextHeightTo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long effectiveBalanceAPL;
}
