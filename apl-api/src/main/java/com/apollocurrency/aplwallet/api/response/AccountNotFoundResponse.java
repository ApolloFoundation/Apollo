/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@Setter
@Getter
@ToString(callSuper = true)
public class AccountNotFoundResponse extends ResponseBase {
    private String account;
    private String accountRS;
    @JsonProperty("is2FA")
    private boolean is2FA;

    public AccountNotFoundResponse(ResponseBase response) {
        super(response);
    }
}
