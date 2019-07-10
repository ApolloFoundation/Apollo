/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Data class for interaction with trade table in the database
 * @author Serhiy Lymar
 */

@Getter @Setter
public class DexTradeEntry {

    private long dbId;
    private long transactionID;
    private long senderOfferID; 
    private long receiverOfferID;
    private byte senderOfferType; 
    private byte senderOfferCurrency; 
    private long senderOfferAmount;
    private byte pairCurrency;
    private BigDecimal pairRate;
    private Integer finishTime;
    private Integer height; 

    public DexTradeInfoDto toDto(){       
        DexTradeInfoDto dexTradeInfoDto = new DexTradeInfoDto();        
        dexTradeInfoDto.dbId = this.dbId;
        dexTradeInfoDto.transactionID = this.transactionID;
        dexTradeInfoDto.senderOfferID = this.senderOfferID; 
        dexTradeInfoDto.receiverOfferID = this.receiverOfferID;   
        dexTradeInfoDto.senderOfferType = this.senderOfferType; 
        dexTradeInfoDto.senderOfferCurrency = this.senderOfferCurrency; 
        dexTradeInfoDto.senderOfferAmount = this.senderOfferAmount;
        dexTradeInfoDto.pairCurrency = this.pairCurrency;
        dexTradeInfoDto.pairRate = this.pairRate;
        dexTradeInfoDto.finishTime = this.finishTime;
        dexTradeInfoDto.height = this.height;         
        return dexTradeInfoDto;
    }
   
}
