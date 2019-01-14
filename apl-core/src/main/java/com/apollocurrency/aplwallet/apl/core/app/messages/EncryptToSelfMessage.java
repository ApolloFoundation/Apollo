/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.messages;

import java.nio.ByteBuffer;

import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

public class EncryptToSelfMessage extends AbstractEncryptedMessage {

    private static final String appendixName = "EncryptToSelfMessage";

    public static EncryptToSelfMessage parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        if (((JSONObject)attachmentData.get("encryptToSelfMessage")).get("data") == null) {
            return new UnencryptedEncryptToSelfMessage(attachmentData);
        }
        return new EncryptToSelfMessage(attachmentData);
    }

    public EncryptToSelfMessage(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
    }

    public EncryptToSelfMessage(JSONObject attachmentData) {
        super(attachmentData, (JSONObject)attachmentData.get("encryptToSelfMessage"));
    }

    public EncryptToSelfMessage(EncryptedData encryptedData, boolean isText, boolean isCompressed) {
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