/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import org.json.simple.JSONObject;

/**
 * It's a simple interface for signature
 */
public interface Signature {
    int ECDSA_SIGNATURE_SIZE = 64;

    byte[] bytes();

    int getSize();

    default String getJsonString() {
        return getJsonObject().toJSONString();
    }

    JSONObject getJsonObject();

    /**
     * Return true if signature is already verified.
     * Used to increase the speed of the verification routine and considered as a simple cache
     *
     * @return
     */
    boolean isVerified();

}
