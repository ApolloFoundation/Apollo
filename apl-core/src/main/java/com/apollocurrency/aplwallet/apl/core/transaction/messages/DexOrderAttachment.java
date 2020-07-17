/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class DexOrderAttachment extends AbstractAttachment {
    private byte type;
    private byte orderCurrency;
    private long orderAmount;
    private byte pairCurrency;
    private byte status;
    //TODO change it on double.
    private long pairRate;
    private int finishTime;

    public DexOrderAttachment(DexOrder order) {
        this.type = Byte.valueOf(String.valueOf(order.getType().ordinal()));
        this.orderCurrency = Byte.valueOf(String.valueOf(order.getOrderCurrency().ordinal()));
        this.orderAmount = order.getOrderAmount();
        this.pairCurrency = Byte.valueOf(String.valueOf(order.getPairCurrency().ordinal()));
        //TODO change on double.
        this.pairRate = EthUtil.ethToGwei(order.getPairRate());
        this.finishTime = order.getFinishTime();
        this.status = Byte.valueOf(String.valueOf(order.getStatus().ordinal()));
    }

    public DexOrderAttachment(ByteBuffer buffer) {
        this.type = buffer.get();
        this.orderCurrency = buffer.get();
        this.orderAmount = buffer.getLong();
        this.pairCurrency = buffer.get();
        this.pairRate = buffer.getLong();
        this.status = buffer.get();
        this.finishTime = buffer.getInt();
    }

    public DexOrderAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.type = Byte.valueOf(String.valueOf(attachmentData.get("type")));
        this.orderCurrency = Byte.valueOf(String.valueOf(attachmentData.get("offerCurrency")));
        this.orderAmount = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("offerAmount")));
        this.pairCurrency = Byte.valueOf(String.valueOf(attachmentData.get("pairCurrency")));
        this.pairRate = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("pairRate")));
        this.status = Byte.valueOf(String.valueOf(attachmentData.get("status")));
        this.finishTime = Integer.valueOf(String.valueOf(attachmentData.get("finishTime")));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionType() {
        return TransactionTypes.TransactionTypeSpec.DEX_ORDER;
    }

    @Override
    public int getMySize() {
        return 1 + 1 + 8 + 1 + 8 + 1 + 4;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(this.type);
        buffer.put(this.orderCurrency);
        buffer.putLong(this.orderAmount);
        buffer.put(this.pairCurrency);
        buffer.putLong(this.pairRate);
        buffer.put(this.status);
        buffer.putInt(this.finishTime);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("type", this.type);
        json.put("offerCurrency", this.orderCurrency);
        json.put("offerAmount", this.orderAmount);
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

    public byte getOrderCurrency() {
        return orderCurrency;
    }

    public void setOrderCurrency(byte orderCurrency) {
        this.orderCurrency = orderCurrency;
    }

    public long getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(long orderAmount) {
        this.orderAmount = orderAmount;
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
