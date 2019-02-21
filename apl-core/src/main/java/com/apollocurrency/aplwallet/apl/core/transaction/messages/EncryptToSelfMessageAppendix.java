/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public class EncryptToSelfMessageAppendix extends AbstractEncryptedMessageAppendix {

    private static final String appendixName = "EncryptToSelfMessage";

    public static EncryptToSelfMessageAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        if (((JSONObject)attachmentData.get("encryptToSelfMessage")).get("data") == null) {
            return new UnencryptedEncryptToSelfMessageAppendix(attachmentData);
        }
        return new EncryptToSelfMessageAppendix(attachmentData);
    }

    public EncryptToSelfMessageAppendix(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
    }

    public EncryptToSelfMessageAppendix(JSONObject attachmentData) {
        super(attachmentData, (JSONObject)attachmentData.get("encryptToSelfMessage"));
    }

    public EncryptToSelfMessageAppendix(EncryptedData encryptedData, boolean isText, boolean isCompressed) {
        super(encryptedData, isText, isCompressed);
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    void putMyJSON(JSONObject json) {
        JSONObject encryptToSelfMessageJSON = new JSONObject();
        super.putMyJSON(encryptToSelfMessageJSON);
        json.put("encryptToSelfMessage", encryptToSelfMessageJSON);
    }

}