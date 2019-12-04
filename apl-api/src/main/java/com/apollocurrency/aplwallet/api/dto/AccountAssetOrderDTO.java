package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountAssetOrderDTO extends BaseDTO {
    private String quantityATU;
    private String priceATM;
    private String accountRS;
    private String asset;
    private String type;
    private String account;
    private String order;
    private Long height;
    private Long transactionHeight;
}
