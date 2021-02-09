/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

public class EncryptedMessageAppendix extends AbstractEncryptedMessageAppendix {

    static final String appendixName = "EncryptedMessage";

    public EncryptedMessageAppendix(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
    }

    public EncryptedMessageAppendix(RlpReader reader) {
        super(reader);
    }

    public EncryptedMessageAppendix(JSONObject attachmentData) {
        super(attachmentData, (Map<?,?>) attachmentData.get("encryptedMessage"));
    }

    public EncryptedMessageAppendix(EncryptedData encryptedData, boolean isText, boolean isCompressed) {
        super(encryptedData, isText, isCompressed);
    }

    public static EncryptedMessageAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        if (((Map<?,?>) attachmentData.get("encryptedMessage")).get("data") == null) {
            throw new RuntimeException("Unencrypted message is not supported");
        }
        return new EncryptedMessageAppendix(attachmentData);
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        JSONObject encryptedMessageJSON = new JSONObject();
        super.putMyJSON(encryptedMessageJSON);
        json.put("encryptedMessage", encryptedMessageJSON);
    }

    @Override
    public void performFullValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        super.performFullValidation(transaction, blockHeight);
        if (transaction.getRecipientId() == 0) {
            throw new AplException.NotValidException("Encrypted messages cannot be attached to transactions with no recipient");
        }
    }

    @Override
    public void performLightweightValidation(Transaction transaction, int blockcHeight) {
        throw new UnsupportedOperationException("Validation for message appendix is not supported, use separate class");
    }

    @Override
    public int getAppendixFlag() {
        return 0x02;
    }

}