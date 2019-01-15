package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.LessorDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountLessorsResponse {
    public LessorDTO[] lessorDTOS;
    public String accountRS;
    public long requestProcessingTime;
    public String account;
    public long height;
}
