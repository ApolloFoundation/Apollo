/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import javax.inject.Singleton;

@Singleton
public class EncryptToSelfMessageAppendixParser implements AppendixParser<EncryptToSelfMessageAppendix> {

    @Override
    public EncryptToSelfMessageAppendix parse(JSONObject jsonData) {
        if (!Appendix.hasAppendix(EncryptToSelfMessageAppendix.appendixName, jsonData)) {
            return null;
        }
        if (((JSONObject) jsonData.get("encryptToSelfMessage")).get("data") == null) {
            throw new RuntimeException("Unencrypted message to self is not supported");
        }
        return new EncryptToSelfMessageAppendix(jsonData);
    }
}
