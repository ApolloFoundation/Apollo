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
public final class MessagingAliasAssignment extends AbstractAttachment {
    
    final String aliasName;
    final String aliasURI;

    public MessagingAliasAssignment(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim();
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
    }

    public MessagingAliasAssignment(JSONObject attachmentData) {
        super(attachmentData);
        aliasName = Convert.nullToEmpty((String) attachmentData.get("alias")).trim();
        aliasURI = Convert.nullToEmpty((String) attachmentData.get("uri")).trim();
    }

    public MessagingAliasAssignment(String aliasName, String aliasURI) {
        this.aliasName = aliasName.trim();
        this.aliasURI = aliasURI.trim();
    }

    @Override
    int getMySize() {
        return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] alias = Convert.toBytes(this.aliasName);
        byte[] uri = Convert.toBytes(this.aliasURI);
        buffer.put((byte) alias.length);
        buffer.put(alias);
        buffer.putShort((short) uri.length);
        buffer.put(uri);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("alias", aliasName);
        attachment.put("uri", aliasURI);
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.ALIAS_ASSIGNMENT;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getAliasURI() {
        return aliasURI;
    }
    
}
