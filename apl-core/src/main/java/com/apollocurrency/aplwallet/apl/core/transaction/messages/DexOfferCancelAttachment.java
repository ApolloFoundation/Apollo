package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class DexOfferCancelAttachment extends AbstractAttachment {

    private long transactionId;

    public DexOfferCancelAttachment(long transactionId) {
        this.transactionId = transactionId;
    }

    public DexOfferCancelAttachment(ByteBuffer buffer) {
        super(buffer);
        this.transactionId = buffer.getLong();
    }

    public DexOfferCancelAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.transactionId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("transactionId")));
    }

    @Override
    int getMySize() {
        return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(transactionId);
    }

    @Override
    void putMyJSON(JSONObject json) {
        json.put("transactionId", this.transactionId);
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_CANCEL_OFFER_TRANSACTION;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }
}
