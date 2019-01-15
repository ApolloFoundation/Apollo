package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigInteger;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTransactionIdsResponse extends ResponseBase{
    public List<BigInteger> transactionIds;
    public List<BigInteger> unconfirmedTransactionIds;
}
