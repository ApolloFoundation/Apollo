/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Map;

public class EncryptToSelfMessageAppendix extends AbstractEncryptedMessageAppendix {

    static final String appendixName = "EncryptToSelfMessage";

    public EncryptToSelfMessageAppendix(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
    }

    public EncryptToSelfMessageAppendix(JSONObject attachmentData) {
        super(attachmentData, (Map<?,?>) attachmentData.get("encryptToSelfMessage"));
    }

    public EncryptToSelfMessageAppendix(EncryptedData encryptedData, boolean isText, boolean isCompressed) {
        super(encryptedData, isText, isCompressed);
    }

    public static EncryptToSelfMessageAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        if (((Map<?,?>) attachmentData.get("encryptToSelfMessage")).get("data") == null) {
            throw new RuntimeException("Unencrypted message to self is not supported");
        }
        return new EncryptToSelfMessageAppendix(attachmentData);
    }

    @Override
    public void performLightweightValidation(Transaction transaction, int blockcHeight) {
        throw new UnsupportedOperationException("Validation for message appendix is not supported, use separate class");
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    public void putMyJSON(JSONObject json) {
        JSONObject encryptToSelfMessageJSON = new JSONObject();
        super.putMyJSON(encryptToSelfMessageJSON);
        json.put("encryptToSelfMessage", encryptToSelfMessageJSON);
    }

}