package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.DEX_CLOSE_ORDER;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DexCloseOrderAttachment extends AbstractAttachment {

    private final long contractId;

    public DexCloseOrderAttachment(ByteBuffer buffer) {
        super(buffer);
        this.contractId = buffer.getLong();
    }

    public DexCloseOrderAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.contractId = Convert.parseUnsignedLong((String) attachmentData.get("contractId"));
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(this.contractId);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("contractId", Long.toUnsignedString(this.getContractId()));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return DEX_CLOSE_ORDER;
    }
}
