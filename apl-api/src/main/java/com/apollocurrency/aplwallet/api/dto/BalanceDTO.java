package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDTO extends BaseDTO {
    private Long balanceATM;
    private Long forgedBalanceATM;
    private Long requestProcessingTime;
    private Long unconfirmedBalanceATM;
    private Long guaranteedBalanceATM;

}
