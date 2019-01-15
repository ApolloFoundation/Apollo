package com.apollocurrency.aplwallet.api.response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class HallmarkResponse extends ResponseBase {
    public String account;
    public String accountRS;
    public String host;
    public Integer port;
    public Integer weight;
    public String date;
    public Boolean valid;



}
