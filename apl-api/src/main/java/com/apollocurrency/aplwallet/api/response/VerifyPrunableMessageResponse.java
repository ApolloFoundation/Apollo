package com.apollocurrency.aplwallet.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyPrunableMessageResponse extends ResponseBase{
    public long versionPrunablePlainMessage;
    public boolean verify;
    public boolean messageIsText;
    public String messageHash;
    public String message;
}
