package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.DEX_TRANSFER_MONEY;

@Getter
@Builder
@AllArgsConstructor
public class DexControlOfFrozenMoneyAttachment extends AbstractAttachment {

    private final long contractId;
    private final long offerAmount; // measured in ATM

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
    public TransactionTypes.TransactionTypeSpec getTransactionType() {
        return DEX_TRANSFER_MONEY;
    }
}
