/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.info;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Data
public class AccountEffectiveBalanceDto {
    public long balanceATM = 0;
    public long unconfirmedBalanceATM = 0;
    public long forgedBalanceATM = 0;
    public Long effectiveBalanceAPL;
    public Long guaranteedBalanceATM;
    public String account;
    public String accountRS;
}
