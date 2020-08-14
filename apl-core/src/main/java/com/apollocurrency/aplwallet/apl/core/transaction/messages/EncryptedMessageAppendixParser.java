/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import javax.inject.Singleton;

@Singleton
public class EncryptedMessageAppendixParser implements AppendixParser<EncryptedMessageAppendix> {
    @Override
    public EncryptedMessageAppendix parse(JSONObject jsonData) {
        if (!Appendix.hasAppendix(EncryptedMessageAppendix.appendixName, jsonData)) {
            return null;
        }
        if (((JSONObject) jsonData.get("encryptedMessage")).get("data") == null) {
            throw new RuntimeException("Unencrypted message is not supported");
        }
        return new EncryptedMessageAppendix(jsonData);
    }
}
