/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class DigitalGoodsFeedback extends AbstractAttachment {

    final long purchaseId;

    public DigitalGoodsFeedback(ByteBuffer buffer) {
        super(buffer);
        this.purchaseId = buffer.getLong();
    }

    public DigitalGoodsFeedback(JSONObject attachmentData) {
        super(attachmentData);
        this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
    }

    public DigitalGoodsFeedback(long purchaseId) {
        this.purchaseId = purchaseId;
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(purchaseId);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("purchase", Long.toUnsignedString(purchaseId));
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_FEEDBACK;
    }

    public long getPurchaseId() {
        return purchaseId;
    }

}
