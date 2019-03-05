/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.NotValidException;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class MessagingAccountProperty extends AbstractAttachment {
    
    final String property;
    final String value;

    public MessagingAccountProperty(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.property = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH).trim();
            this.value = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH).trim();
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public MessagingAccountProperty(JSONObject attachmentData) {
        super(attachmentData);
        this.property = Convert.nullToEmpty((String) attachmentData.get("property")).trim();
        this.value = Convert.nullToEmpty((String) attachmentData.get("value")).trim();
    }

    public MessagingAccountProperty(String property, String value) {
        this.property = property.trim();
        this.value = Convert.nullToEmpty(value).trim();
    }

    @Override
    int getMySize() {
        return 1 + Convert.toBytes(property).length + 1 + Convert.toBytes(value).length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] property = Convert.toBytes(this.property);
        byte[] value = Convert.toBytes(this.value);
        buffer.put((byte) property.length);
        buffer.put(property);
        buffer.put((byte) value.length);
        buffer.put(value);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("property", property);
        attachment.put("value", value);
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.ACCOUNT_PROPERTY;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }
    
}
