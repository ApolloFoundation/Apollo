/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto.account;

import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AccountEffectiveBalanceDto extends BalanceDTO {
    public Long effectiveBalanceAPL = 0L;
    public String account;
    public String accountRS;

    public AccountEffectiveBalanceDto(Long balanceATM, Long forgedBalanceATM,
                                      Long requestProcessingTime, Long unconfirmedBalanceATM,
                                      Long guaranteedBalanceATM,
                                      String account, String accountRS) {
        super(balanceATM, forgedBalanceATM, requestProcessingTime, unconfirmedBalanceATM, guaranteedBalanceATM);
        this.account = account;
        this.accountRS = accountRS;
    }
}
