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
   
}
