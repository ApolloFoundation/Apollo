/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */
package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.account.AccountCurrencyDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AccountCurrencyResponse extends ResponseBase {

    private List<AccountCurrencyDTO> accountCurrencies;

    public AccountCurrencyResponse(List<AccountCurrencyDTO> accountCurrencies) {
        this.accountCurrencies = accountCurrencies;
    }
}
