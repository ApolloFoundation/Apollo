/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public class EncryptedMessage extends AbstractEncryptedMessage {

    private static final String appendixName = "EncryptedMessage";

    public static EncryptedMessage parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        if (((JSONObject)attachmentData.get("encryptedMessage")).get("data") == null) {
            return new UnencryptedEncryptedMessage(attachmentData);
        }
        return new EncryptedMessage(attachmentData);
    }

    public EncryptedMessage(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
    }

    public EncryptedMessage(JSONObject attachmentData) {
        super(attachmentData, (JSONObject)attachmentData.get("encryptedMessage"));
    }

    public EncryptedMessage(EncryptedData encryptedData, boolean isText, boolean isCompressed) {
        super(encryptedData, isText, isCompressed);
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    void putMyJSON(JSONObject json) {
        JSONObject encryptedMessageJSON = new JSONObject();
        super.putMyJSON(encryptedMessageJSON);
        json.put("encryptedMessage", encryptedMessageJSON);
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
            super.validate(transaction, blockHeight);
        if (transaction.getRecipientId() == 0) {
            throw new AplException.NotValidException("Encrypted messages cannot be attached to transactions with no recipient");
        }
    }

}