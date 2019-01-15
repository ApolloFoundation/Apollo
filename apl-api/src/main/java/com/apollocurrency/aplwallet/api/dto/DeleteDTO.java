package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteDTO {
    public String assetDelete;
    public String accountRS;
    public String quantityATU;
    public String asset;
    public String account;
    public Long height;
    public Boolean phased;
}
