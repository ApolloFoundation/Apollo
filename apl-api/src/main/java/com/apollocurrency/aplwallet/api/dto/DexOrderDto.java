/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name="DexOffer", description="Dex offer")
public class DexOrderDto {

    /**
     * id for entity offer is a transaction id.
     */
    @Schema(name="ID of order", description="Identificator of order")
    public String id;
    @Schema(name="ID of sender(owner)", description="Identificator of an sender")
    public String accountId;
    @Schema(name="Wallet address", description="address of an offer currency.")
    public String fromAddress;
    @Schema(name="Wallet address", description="address to receive currency")
    public String toAddress;

    @Schema(name="Exchange type", description="buy / sell")
    public Integer type;
    //todo rename to orderCurrency (fix on UI)
    @Schema(name="Offer currency", description="eth / pax / apl")
    public Integer offerCurrency;
    //todo rename to orderAmount (fix on UI)
    @Schema(name="Offer amount", description="Amount of currency for exchange ")
    public Long offerAmount;
    @Schema(name="Pair rate", description="Rate to exchange")
    public Long pairRate;

    @Schema(name="Pair currency", description="Opposite currency to exchange")
    public Integer pairCurrency;
    @Schema(name="Order status", description="Order status (Open/Close/Expired)")
    public Integer status;
    @Schema(name="Finish time", description="Order finish time")
    public Integer finishTime;

    @Schema(name = "Has frozen money", description = "Determine existence of money freezing for order. For fully initialized orders should be true")
    public Boolean hasFrozenMoney;

}
