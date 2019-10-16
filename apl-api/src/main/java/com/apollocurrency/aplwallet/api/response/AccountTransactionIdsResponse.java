package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class AccountTransactionIdsResponse extends ResponseBase {
    private List<BigInteger> transactionIds;
    private List<BigInteger> unconfirmedTransactionIds;
}
