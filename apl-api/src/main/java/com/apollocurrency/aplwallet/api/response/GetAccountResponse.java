package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class GetAccountResponse extends ResponseBase {
    private String balanceATM;
    private String forgedBalanceATM;
    private String accountRS;
    private String publicKey;
    private String unconfirmedBalanceATM;
    private String account;
    private Long numberOfBlocks;
    private Long guaranteedBalanceATM;
    private Long effectiveBalanceAPL;
    private Long requestProcessingTime;
    private String name;
    private Boolean is2FA;
    private BigInteger currentLessee;
    private String currentLesseeRS;
    private Long currentLeasingHeightTo;
    private Long currentLeasingHeightFrom;
    private String description;

}
