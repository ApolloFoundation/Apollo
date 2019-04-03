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
public final class MessagingAccountInfo extends AbstractAttachment {
    
    final String name;
    final String description;

    public MessagingAccountInfo(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public MessagingAccountInfo(JSONObject attachmentData) {
        super(attachmentData);
        this.name = Convert.nullToEmpty((String) attachmentData.get("name"));
        this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
    }

    public MessagingAccountInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    int getMySize() {
        return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] name = Convert.toBytes(this.name);
        byte[] description = Convert.toBytes(this.description);
        buffer.put((byte) name.length);
        buffer.put(name);
        buffer.putShort((short) description.length);
        buffer.put(description);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("name", name);
        attachment.put("description", description);
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.ACCOUNT_INFO;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    
}
