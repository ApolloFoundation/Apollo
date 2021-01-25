/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class TransactionUtils {
    public static boolean convertAppendixToString(StringBuilder builder, Appendix appendix) {
        if (appendix != null) {
            JSONObject json = appendix.getJSONObject();
            if (json != null) {
                builder.append(json.toJSONString());
                return true;
            }
        }
        return false;
    }

}
