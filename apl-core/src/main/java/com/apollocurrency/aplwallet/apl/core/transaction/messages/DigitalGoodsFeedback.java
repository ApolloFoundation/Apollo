/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.DigitalGoods;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
    int getMySize() {
        return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(purchaseId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("purchase", Long.toUnsignedString(purchaseId));
    }

    @Override
    public TransactionType getTransactionType() {
        return DigitalGoods.FEEDBACK;
    }

    public long getPurchaseId() {
        return purchaseId;
    }
    
}
