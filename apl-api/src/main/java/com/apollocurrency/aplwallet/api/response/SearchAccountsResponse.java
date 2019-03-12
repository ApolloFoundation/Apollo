package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchAccountsResponse extends ResponseBase{
    //public AccountDTO[] accountDTOS;
    // TODO : added for api testing
     public AccountDTO[] accounts;

}
