/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class EncryptedMessageAppendixParser implements AppendixParser<EncryptedMessageAppendix> {
    @Override
    public EncryptedMessageAppendix parse(JSONObject jsonData) {
        if (!Appendix.hasAppendix(EncryptedMessageAppendix.appendixName, jsonData)) {
            return null;
        }
        if (((Map<?, ?>) jsonData.get("encryptedMessage")).get("data") == null) {
            throw new RuntimeException("Unencrypted message is not supported");
        }
        return new EncryptedMessageAppendix(jsonData);
    }

    @Override
    public Class<EncryptedMessageAppendix> forClass() {
        return EncryptedMessageAppendix.class;
    }
}
