/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import jakarta.inject.Singleton;

@Singleton
public class PhasingAppendixParser implements AppendixParser<PhasingAppendix> {
    @Override
    public PhasingAppendix parse(JSONObject jsonData) {
        if (Appendix.hasAppendix("Phasing", jsonData)) {
            return new PhasingAppendix(jsonData);
        } else if (Appendix.hasAppendix("Phasing_V2", jsonData)) {
            return new PhasingAppendixV2(jsonData);
        }
        return null;
    }

    @Override
    public Class<PhasingAppendix> forClass() {
        return PhasingAppendix.class;
    }
}
