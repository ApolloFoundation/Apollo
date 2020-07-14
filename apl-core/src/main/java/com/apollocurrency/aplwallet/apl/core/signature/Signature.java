/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import org.json.simple.JSONObject;

/**
 * It's a simple interface for signature
 */
public interface Signature {

    boolean isCanonical();

    byte[] bytes();

    int getSize();

    default String getJsonString() {
        return getJsonObject().toJSONString();
    }

    JSONObject getJsonObject();

}
