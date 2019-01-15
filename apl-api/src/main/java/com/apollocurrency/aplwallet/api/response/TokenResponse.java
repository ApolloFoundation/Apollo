package com.apollocurrency.aplwallet.api.response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class TokenResponse extends ResponseBase{
    public String account;
    public String accountRS;
    public Integer timestamp;
    public Boolean valid;
    public String token;

}
