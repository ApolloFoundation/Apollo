/*
 * Copyright © 2018-2021 Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.dto.account;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew.zinchenko@gmail.com
 */
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode
public class CurrenciesWalletsDTO extends BaseDTO {
    private String passphrase;
    private List<CurrencyWalletsDTO> currencies = new ArrayList<>();

    public void addWallet(CurrencyWalletsDTO dto) {
        currencies.add(dto);
    }

}
