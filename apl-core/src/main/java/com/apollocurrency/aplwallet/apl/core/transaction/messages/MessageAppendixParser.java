/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import jakarta.inject.Singleton;

@Singleton
public class MessageAppendixParser implements AppendixParser<MessageAppendix> {
    @Override
    public MessageAppendix parse(JSONObject jsonData) {
        if (!Appendix.hasAppendix(MessageAppendix.appendixName, jsonData)) {
            return null;
        }
        return new MessageAppendix(jsonData);
    }

    @Override
    public Class<MessageAppendix> forClass() {
        return MessageAppendix.class;
    }
}
