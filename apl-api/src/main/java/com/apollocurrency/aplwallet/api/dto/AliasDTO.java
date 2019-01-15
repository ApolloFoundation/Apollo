package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliasDTO {
    public String aliasURI;
    public String aliasName;
    public String accountRS;
    public String alias;
    public String account;
    public long timestamp;
}
