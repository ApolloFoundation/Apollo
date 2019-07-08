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
public class DexContractAttachment extends AbstractAttachment {

    private Long orderId;
    private Long counterOrderId;
    /**
     * Hash from secret key. sha256(key)
     */
    private String secretHash;

    public DexContractAttachment(ByteBuffer buffer) {
        super(buffer);
        this.orderId = buffer.getLong();
        this.counterOrderId = buffer.getLong();

        byte hex[] = new byte[32];
        buffer.get(hex);
        this.secretHash = Convert.toHexString(hex);
    }

    public DexContractAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("orderId")));
        this.counterOrderId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("counterOrderId")));
        this.secretHash = String.valueOf(attachmentData.get("secretHash"));

    }

    @Override
    public int getMySize() {
        //secretHash fix size (hex(Sha256()) - 32 bites)
        return 8 + 8 + 32;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.orderId);
        buffer.putLong(this.counterOrderId);
        buffer.put(Convert.parseHexString(this.secretHash));
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("orderId", this.getOrderId());
        json.put("counterOrderId", this.getCounterOrderId());
        json.put("secretHash", this.secretHash);
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_CONTRACT_TRANSACTION;
    }
}
