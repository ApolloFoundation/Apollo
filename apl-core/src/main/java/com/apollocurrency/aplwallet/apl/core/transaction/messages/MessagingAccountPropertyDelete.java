/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.types.messaging.MessagingTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class MessagingAccountPropertyDelete extends AbstractAttachment {

    final long propertyId;

    public MessagingAccountPropertyDelete(ByteBuffer buffer) {
        super(buffer);
        this.propertyId = buffer.getLong();
    }

    public MessagingAccountPropertyDelete(JSONObject attachmentData) {
        super(attachmentData);
        this.propertyId = Convert.parseUnsignedLong((String) attachmentData.get("property"));
    }

    public MessagingAccountPropertyDelete(long propertyId) {
        this.propertyId = propertyId;
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(propertyId);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("property", Long.toUnsignedString(propertyId));
    }

    @Override
    public TransactionType getTransactionType() {
        return MessagingTransactionType.ACCOUNT_PROPERTY_DELETE;
    }

    public long getPropertyId() {
        return propertyId;
    }

}
