/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import org.json.simple.JSONObject;

import javax.inject.Singleton;

import static com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix.hasAppendix;

@Singleton
public class PrunablePlainMessageAppendixParser implements AppendixParser<PrunablePlainMessageAppendix>{

    @Override
    public PrunablePlainMessageAppendix parse(JSONObject jsonData) {
        if (!hasAppendix(PrunablePlainMessageAppendix.APPENDIX_NAME, jsonData)) {
            return null;
        }
        return new PrunablePlainMessageAppendix(jsonData);
    }
}
