/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import java.math.BigDecimal;

/**
 * Data class for interaction with trade table in the database
 * @author Serhiy Lymar
 */
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

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }
    
    public long getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(long transactionID) {
        this.transactionID = transactionID;
    }

    public long getSenderOfferID() {
        return senderOfferID;
    }

    public void setSenderOfferID(long senderOfferID) {
        this.senderOfferID = senderOfferID;
    }

    public long getReceiverOfferID() {
        return receiverOfferID;
    }

    public void setReceiverOfferID(long receiverOfferID) {
        this.receiverOfferID = receiverOfferID;
    }

    public byte getSenderOfferType() {
        return senderOfferType;
    }

    public void setSenderOfferType(byte senderOfferType) {
        this.senderOfferType = senderOfferType;
    }

    public byte getSenderOfferCurrency() {
        return senderOfferCurrency;
    }

    public void setSenderOfferCurrency(byte senderOfferCurrency) {
        this.senderOfferCurrency = senderOfferCurrency;
    }

    public long getSenderOfferAmount() {
        return senderOfferAmount;
    }

    public void setSenderOfferAmount(long senderOfferAmount) {
        this.senderOfferAmount = senderOfferAmount;
    }

    public byte getPairCurrency() {
        return pairCurrency;
    }

    public void setPairCurrency(byte pairCurrency) {
        this.pairCurrency = pairCurrency;
    }

    public BigDecimal getPairRate() {
        return pairRate;
    }

    public void setPairRate(BigDecimal pairRate) {
        this.pairRate = pairRate;
    }

    public Integer getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Integer finishTime) {
        this.finishTime = finishTime;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
        
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
