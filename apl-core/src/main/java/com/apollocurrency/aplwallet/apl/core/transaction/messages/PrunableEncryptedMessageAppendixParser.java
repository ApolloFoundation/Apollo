/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class PrunableEncryptedMessageAppendixParser implements AppendixParser<PrunableEncryptedMessageAppendix> {
    @Override
    public PrunableEncryptedMessageAppendix parse(JSONObject jsonData) {
        if (!Appendix.hasAppendix(PrunableEncryptedMessageAppendix.APPENDIX_NAME, jsonData)) {
            return null;
        }
        Map<?,?> encryptedMessageJSON = (Map<?,?>) jsonData.get("encryptedMessage");
        if (encryptedMessageJSON != null && encryptedMessageJSON.get("data") == null) {
            throw new RuntimeException("Unencrypted prunable message is not supported");
        }
        return new PrunableEncryptedMessageAppendix(jsonData);
    }

    @Override
    public Class<PrunableEncryptedMessageAppendix> forClass() {
        return PrunableEncryptedMessageAppendix.class;
    }
}
