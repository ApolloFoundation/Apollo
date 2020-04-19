/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import org.json.simple.JSONObject;

import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class PhasingAppendixFactory {

    public static PhasingAppendix build(ByteBuffer buffer) {
        byte version = buffer.get();

        switch (version) {
            case 1:
                return new PhasingAppendix(buffer);
            case 2:
                return new PhasingAppendixV2(buffer);
            default:
                throw new UnsupportedOperationException("Version: " + version + ", not supported by PhasingAppendixFactory");
        }
    }

    public static PhasingAppendix parse(JSONObject attachmentData) {
        if (Appendix.hasAppendix("Phasing", attachmentData)) {
            return new PhasingAppendix(attachmentData);
        } else if (Appendix.hasAppendix("Phasing_V2", attachmentData)) {
            return new PhasingAppendixV2(attachmentData);
        }
        return null;
    }

}
