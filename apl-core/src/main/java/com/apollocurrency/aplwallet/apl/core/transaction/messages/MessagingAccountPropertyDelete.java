/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
    int getMySize() {
        return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(propertyId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("property", Long.toUnsignedString(propertyId));
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.ACCOUNT_PROPERTY_DELETE;
    }

    public long getPropertyId() {
        return propertyId;
    }
    
}
