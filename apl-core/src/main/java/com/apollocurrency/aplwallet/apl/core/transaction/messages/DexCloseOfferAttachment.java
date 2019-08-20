package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
public class DexCloseOfferAttachment extends AbstractAttachment {

    private long orderId;

    public DexCloseOfferAttachment(ByteBuffer buffer) {
        super(buffer);
        this.orderId = buffer.getLong();
    }

    public DexCloseOfferAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("orderId")));
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.orderId);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("orderId", Long.toUnsignedString(this.getOrderId()));
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_CLOSE_OFFER;
    }
}
