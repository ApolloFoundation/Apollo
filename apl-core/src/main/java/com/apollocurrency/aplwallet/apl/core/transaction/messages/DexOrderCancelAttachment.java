package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class DexOrderCancelAttachment extends AbstractAttachment {

    private long orderId;

    public DexOrderCancelAttachment(long orderId) {
        this.orderId = orderId;
    }

    public DexOrderCancelAttachment(ByteBuffer buffer) {
        super(buffer);
        this.orderId = buffer.getLong();
    }

    public DexOrderCancelAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("transactionId")));
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(orderId);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("transactionId", Long.toUnsignedString(this.orderId));
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_CANCEL_ORDER_TRANSACTION;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }
}
