/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.response.ResponseBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class ForgingDetails extends ResponseBase {
    private Long deadline;
    private Long hitTime;
    private Long remaining;
    private Boolean foundAndStopped;
    private BasicAccount account;
    private String accountRS;
    private int forgersCount;

}