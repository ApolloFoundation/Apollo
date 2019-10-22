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
public class AssetTradeDTO extends BaseDTO {
    private String seller;
    private String bidOrder;
    private String priceATM;
    private String sellerRS;
    private String buyer;
    private String askOrder;
    private String buyerRS;
    private String quantityATU;
    private String block;
    private String asset;
    private Long askOrderHeight;
    private Long bidOrderHeight;
    private String tradeType;
    private Long timestamp;
    private Long height;
}
