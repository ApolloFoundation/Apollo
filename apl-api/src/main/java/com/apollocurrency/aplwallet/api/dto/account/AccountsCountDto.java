/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.account;

import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.api.dto.BaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountsCountDto extends BaseDTO {
    public long totalSupply;
    public long totalNumberOfAccounts;
    public int numberOfTopAccounts;
    public long totalAmountOnTopAccounts;
    public List<AccountEffectiveBalanceDto> topHolders = new ArrayList<>();

    /**
     * Constructor for unit tests.
     * @param totalSupply
     * @param totalNumberOfAccounts
     * @param numberOfTopAccounts
     * @param totalAmountOnTopAccounts
     */
    public AccountsCountDto(long totalSupply, long totalNumberOfAccounts, int numberOfTopAccounts, long totalAmountOnTopAccounts) {
        this.totalSupply = totalSupply;
        this.totalNumberOfAccounts = totalNumberOfAccounts;
        this.numberOfTopAccounts = numberOfTopAccounts;
        this.totalAmountOnTopAccounts = totalAmountOnTopAccounts;
    }
}
