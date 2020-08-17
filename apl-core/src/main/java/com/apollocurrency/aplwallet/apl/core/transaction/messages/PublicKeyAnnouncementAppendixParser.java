/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import javax.inject.Singleton;

@Singleton
public class PublicKeyAnnouncementAppendixParser implements AppendixParser<PublicKeyAnnouncementAppendix> {
    @Override
    public PublicKeyAnnouncementAppendix parse(JSONObject jsonData) {
        if (!Appendix.hasAppendix(PublicKeyAnnouncementAppendix.appendixName, jsonData)) {
            return null;
        }
        return new PublicKeyAnnouncementAppendix(jsonData);
    }
}
