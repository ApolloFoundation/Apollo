package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeDTO {
    public String seller;
    public String bidOrder;
    public String priceATM;
    public String sellerRS;
    public String buyer;
    public String askOrder;
    public String buyerRS;
    public String quantityATU;
    public String block;
    public String asset;
    public Long askOrderHeight;
    public Long bidOrderHeight;
    public String tradeType;
    public Long timestamp;
    public Long height;
}
