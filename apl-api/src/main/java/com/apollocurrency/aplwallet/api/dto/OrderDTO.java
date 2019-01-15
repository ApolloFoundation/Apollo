package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDTO {
    public String quantityATU;
    public String priceATM;
    public String accountRS;
    public String asset;
    public String type;
    public String account;
    public String order;
    public long height;
    public Long transactionHeight;
}
