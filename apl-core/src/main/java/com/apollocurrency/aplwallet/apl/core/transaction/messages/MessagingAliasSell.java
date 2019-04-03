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
public final class MessagingAliasSell extends AbstractAttachment {
    
    final String aliasName;
    final long priceATM;

    public MessagingAliasSell(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        try {
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        } catch (NotValidException ex) {
            throw new AplException.NotValidException(ex.getMessage());
        }
        this.priceATM = buffer.getLong();
    }

    public MessagingAliasSell(JSONObject attachmentData) {
        super(attachmentData);
        this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
        this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
    }

    public MessagingAliasSell(String aliasName, long priceATM) {
        this.aliasName = aliasName;
        this.priceATM = priceATM;
    }

    @Override
    public TransactionType getTransactionType() {
        return Messaging.ALIAS_SELL;
    }

    @Override
    int getMySize() {
        return 1 + Convert.toBytes(aliasName).length + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        byte[] aliasBytes = Convert.toBytes(aliasName);
        buffer.put((byte) aliasBytes.length);
        buffer.put(aliasBytes);
        buffer.putLong(priceATM);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("alias", aliasName);
        attachment.put("priceATM", priceATM);
    }

    public String getAliasName() {
        return aliasName;
    }

    public long getPriceATM() {
        return priceATM;
    }
    
}
