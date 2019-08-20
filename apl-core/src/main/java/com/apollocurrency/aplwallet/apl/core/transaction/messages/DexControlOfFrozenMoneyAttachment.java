package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.exchange.transaction.DEX;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@Data
@Builder
@AllArgsConstructor
public class DexControlOfFrozenMoneyAttachment extends AbstractAttachment {

    private long orderId;
    private boolean hasFrozenMoney;

    public DexControlOfFrozenMoneyAttachment(ByteBuffer buffer) {
        super(buffer);
        this.orderId = buffer.getLong();
        this.hasFrozenMoney = buffer.get() == 1;
    }

    public DexControlOfFrozenMoneyAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("orderId")));
    }

    @Override
    public int getMySize() {
        return 8 + 1;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.orderId);
        buffer.put(hasFrozenMoney ? (byte)1 : (byte)0);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("orderId", Long.toUnsignedString(this.getOrderId()));
        json.put("hasFrozenMoney", this.isHasFrozenMoney());
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_TRANSFER_MONEY_TRANSACTION;
    }
}
