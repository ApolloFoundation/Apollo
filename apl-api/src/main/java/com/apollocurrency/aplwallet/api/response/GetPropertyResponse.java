package com.apollocurrency.aplwallet.api.response;

import com.apollocurrency.aplwallet.api.dto.FbcPropertyDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetPropertyResponse extends ResponseBase{
    public String recipientRS;
    public String recipient;
    public FbcPropertyDTO[] properties;
}

