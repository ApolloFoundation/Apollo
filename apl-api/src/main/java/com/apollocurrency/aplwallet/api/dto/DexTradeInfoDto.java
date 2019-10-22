/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

/**
 *
 * @author Serhiy Lymar
 */

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name="DexTradeInfoDto", description="Dex trade information")
public class DexTradeInfoDto {

    /**
     * id for this entity is a database id.
     */
    @Schema(name="database id", description="Identificator of the entry insidt the database")
    public long dbId;
    @Schema(name="transaction ID", description="Identificator of exchange transaction")
    public long transactionID;
    @Schema(name="sender's offer ID", description="Identificator of exchange offer from the side of sender")
    public long senderOfferID; 
    @Schema(name="receiver's offer ID", description="Identificator of exchange offer from the side of receiver")
    public long receiverOfferID;   
    @Schema(name="Exchange type", description="buy/sell = 0/1")
    public byte senderOfferType; 
    @Schema(name="Type of currency", description="eth/pax/apl = 0/1/2")
    public byte senderOfferCurrency; 
    @Schema(name="Amount of currency for exchange", description="parameter in gwei units")
    public long senderOfferAmount;
    @Schema(name="Type of pair currency", description="eth/pax/apl = 0/1/2")
    public byte pairCurrency;
    @Schema(name="Exchange rate", description="parameter in gwei units")
    public BigDecimal pairRate;
    @Schema(name="Finish time", description="Order finish time")
    public Integer finishTime;
    @Schema(name="Order height", description="Blockchain-related height value")
    public Integer height; 
}
