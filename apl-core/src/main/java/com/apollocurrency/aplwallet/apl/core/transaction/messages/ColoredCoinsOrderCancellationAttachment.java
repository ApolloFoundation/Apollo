/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public abstract class ColoredCoinsOrderCancellationAttachment extends AbstractAttachment {

    final long orderId;

    public ColoredCoinsOrderCancellationAttachment(ByteBuffer buffer) {
        super(buffer);
        this.orderId = buffer.getLong();
    }

    public ColoredCoinsOrderCancellationAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
    }

    public ColoredCoinsOrderCancellationAttachment(long orderId) {
        this.orderId = orderId;
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(orderId);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("order", Long.toUnsignedString(orderId));
    }

    public long getOrderId() {
        return orderId;
    }

}
