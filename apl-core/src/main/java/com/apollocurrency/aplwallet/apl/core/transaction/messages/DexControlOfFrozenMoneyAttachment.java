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

    private long contractId;
    private long offerAmount; // measured in ATM

    public DexControlOfFrozenMoneyAttachment(ByteBuffer buffer) {
        super(buffer);
        this.contractId = buffer.getLong();
        this.offerAmount = buffer.getLong();
    }

    public DexControlOfFrozenMoneyAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.offerAmount = Convert.parseLong(attachmentData.get("offerAmount"));
        this.contractId = Convert.parseUnsignedLong(String.valueOf(attachmentData.get("contractId")));
    }

    @Override
    public int getMySize() {
        return 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.contractId);
        buffer.putLong(this.offerAmount);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("contractId", Long.toUnsignedString(contractId));
        json.put("offerAmount", offerAmount);
    }

    @Override
    public TransactionType getTransactionType() {
        return DEX.DEX_TRANSFER_MONEY_TRANSACTION;
    }
}
