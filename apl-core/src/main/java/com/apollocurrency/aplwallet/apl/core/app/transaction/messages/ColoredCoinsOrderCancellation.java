/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public abstract class ColoredCoinsOrderCancellation extends AbstractAttachment {
    
    final long orderId;

    public ColoredCoinsOrderCancellation(ByteBuffer buffer) {
        super(buffer);
        this.orderId = buffer.getLong();
    }

    public ColoredCoinsOrderCancellation(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
    }

    public ColoredCoinsOrderCancellation(long orderId) {
        this.orderId = orderId;
    }

    @Override
    int getMySize() {
        return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(orderId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("order", Long.toUnsignedString(orderId));
    }

    public long getOrderId() {
        return orderId;
    }
    
}
