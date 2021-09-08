/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.dex.eth.utils.EthUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class DexOrderAttachment extends AbstractAttachment {
    private final byte type;
    private final byte orderCurrency;
    private final long orderAmount;
    private final byte pairCurrency;
    //TODO change it on double.
    private final long pairRate;
    private final byte status;
    private final int finishTime;

    public DexOrderAttachment(DexOrder order) {
        this.type = Byte.parseByte(String.valueOf(order.getType().ordinal()));
        this.orderCurrency = Byte.parseByte(String.valueOf(order.getOrderCurrency().ordinal()));
        this.orderAmount = order.getOrderAmount();
        this.pairCurrency = Byte.parseByte(String.valueOf(order.getPairCurrency().ordinal()));
        //TODO change on double.
        this.pairRate = EthUtil.ethToGwei(order.getPairRate());
        this.status = Byte.parseByte(String.valueOf(order.getStatus().ordinal()));
        this.finishTime = order.getFinishTime();
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
        this.type = Byte.parseByte(String.valueOf(attachmentData.get("type")));
        this.orderCurrency = Byte.parseByte(String.valueOf(attachmentData.get("offerCurrency")));
        this.orderAmount = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("offerAmount")));
        this.pairCurrency = Byte.parseByte(String.valueOf(attachmentData.get("pairCurrency")));
        this.pairRate = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("pairRate")));
        this.status = Byte.parseByte(String.valueOf(attachmentData.get("status")));
        this.finishTime = Integer.parseInt(String.valueOf(attachmentData.get("finishTime")));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
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
}
