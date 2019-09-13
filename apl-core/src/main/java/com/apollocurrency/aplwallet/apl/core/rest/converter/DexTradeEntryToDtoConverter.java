/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;

/**
 *
 * @author Serhiy Lymar
 */
public class DexTradeEntryToDtoConverter implements Converter< DexTradeEntry, DexTradeInfoDto > {

    @Override
    public DexTradeInfoDto apply(DexTradeEntry t) {
        DexTradeInfoDto dexTradeInfoDto = new DexTradeInfoDto();        
        dexTradeInfoDto.dbId = t.getDbId();
        dexTradeInfoDto.transactionID = t.getTransactionID();
        dexTradeInfoDto.senderOfferID = t.getSenderOfferID(); 
        dexTradeInfoDto.receiverOfferID = t.getReceiverOfferID();   
        dexTradeInfoDto.senderOfferType = t.getSenderOfferType();
        dexTradeInfoDto.senderOfferCurrency = t.getSenderOfferCurrency(); 
        dexTradeInfoDto.senderOfferAmount = t.getSenderOfferAmount();
        dexTradeInfoDto.pairCurrency = t.getPairCurrency();
        dexTradeInfoDto.pairRate = t.getPairRate();
        dexTradeInfoDto.finishTime = t.getFinishTime();
        dexTradeInfoDto.height = t.getHeight();         
        return dexTradeInfoDto;
    }



}
