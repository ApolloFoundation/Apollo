/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class DexOfferAttachment extends AbstractAttachment {
    private byte type;
    private byte offerCurrency;
    private long offerAmount;
    private byte pairCurrency;
    private byte status;
    private long pairRate;
    private int finishTime;

    public DexOfferAttachment(DexOffer offer) {
        this.type = Byte.valueOf(String.valueOf(offer.getType().ordinal()));
        this.offerCurrency = Byte.valueOf(String.valueOf(offer.getOfferCurrency().ordinal()));
        this.offerAmount = offer.getOfferAmount();
        this.pairCurrency = Byte.valueOf(String.valueOf(offer.getPairCurrency().ordinal()));
        this.pairRate = offer.getPairRate();
        this.finishTime = offer.getFinishTime();
        this.status = Byte.valueOf(String.valueOf(offer.getStatus().ordinal()));
    }

    public DexOfferAttachment(ByteBuffer buffer) {
        this.type = buffer.get();
        this.offerCurrency = buffer.get();
        this.offerAmount = buffer.getLong();
        this.pairCurrency = buffer.get();
        this.pairRate = buffer.getLong();
        this.status = buffer.get();
        this.finishTime = buffer.getInt();
    }

    public DexOfferAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.type = Byte.valueOf(String.valueOf(attachmentData.get("type")));
        this.offerCurrency = Byte.valueOf(String.valueOf(attachmentData.get("offerCurrency")));
        this.offerAmount = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("offerAmount")));
        this.pairCurrency = Byte.valueOf(String.valueOf(attachmentData.get("pairCurrency")));
        this.pairRate = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("pairRate")));
        this.status = Byte.valueOf(String.valueOf( attachmentData.get("status")));
        this.finishTime = Integer.valueOf(String.valueOf(attachmentData.get("finishTime")));
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_OFFER_TRANSACTION;
    }

    @Override
    public int getMySize() {
        return 1 + 1 + 8 + 1 + 8 + 1 + 4;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(this.type);
        buffer.put(this.offerCurrency);
        buffer.putLong(this.offerAmount);
        buffer.put(this.pairCurrency);
        buffer.putLong(this.pairRate);
        buffer.put(this.status);
        buffer.putInt(this.finishTime);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("type", this.type);
        json.put("offerCurrency", this.offerCurrency);
        json.put("offerAmount", this.offerAmount);
        json.put("pairCurrency", this.pairCurrency);
        json.put("pairRate", this.pairRate);
        json.put("status", this.status);
        json.put("finishTime", this.finishTime);
    }

    @Override
    public byte getVersion() {
        return 1;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getOfferCurrency() {
        return offerCurrency;
    }

    public void setOfferCurrency(byte offerCurrency) {
        this.offerCurrency = offerCurrency;
    }

    public long getOfferAmount() {
        return offerAmount;
    }

    public void setOfferAmount(long offerAmount) {
        this.offerAmount = offerAmount;
    }

    public byte getPairCurrency() {
        return pairCurrency;
    }

    public void setPairCurrency(byte pairCurrency) {
        this.pairCurrency = pairCurrency;
    }

    public long getPairRate() {
        return pairRate;
    }

    public void setPairRate(long pairRate) {
        this.pairRate = pairRate;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}
