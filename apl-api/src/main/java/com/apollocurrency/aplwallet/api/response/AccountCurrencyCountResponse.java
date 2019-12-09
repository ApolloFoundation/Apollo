/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */
package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AccountCurrencyCountResponse extends ResponseBase {

    private Integer numberOfCurrencies;

    public AccountCurrencyCountResponse(Integer numberOfCurrencies) {
        this.numberOfCurrencies = numberOfCurrencies;
    }
}
